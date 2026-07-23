package com.ratel.fm.service.basic;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.ratel.fm.common.BusinessException;
import com.ratel.fm.common.ResponseCode;
import com.ratel.fm.domain.basic.BasicDictionary;
import com.ratel.fm.repository.basic.BasicDictionaryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * 币种快照服务。
 *
 * <p>用于凭证、采购、应收应付等涉金额业务统一解析币种字典，并保存业务发生当时的汇率。</p>
 *
 * <p>开发组织：ratel；开发人员：WenZhang；联系方式：18782945613。</p>
 *
 * @author ratel
 */
@Service
public class CurrencyService {

    /** 币种根字典编码，所有业务币种都维护在该节点下。 */
    public static final String CURRENCY_ROOT_CODE = "CURRENCY";

    /** 金额统一精度，涉金额输入、计算和快照均保留 8 位小数。 */
    public static final int MONEY_SCALE = 8;

    /** 系统默认人民币币种编码。 */
    public static final String DEFAULT_CURRENCY_CODE = "CNY";

    /** 系统默认人民币币种名称。 */
    public static final String DEFAULT_CURRENCY_NAME = "人民币";

    /** 人民币对人民币固定汇率，作为本位币计算和接口兜底值。 */
    private static final BigDecimal CNY_EXCHANGE_RATE = BigDecimal.ONE.setScale(MONEY_SCALE, RoundingMode.HALF_UP);

    /** 汇率请求超时时间，避免外部网络异常长时间阻塞业务操作。 */
    private static final Duration EXCHANGE_RATE_TIMEOUT = Duration.ofSeconds(5);

    /** Frankfurter v2 单币对公开汇率接口地址；该接口返回最新可用参考汇率而非秒级实时行情。 */
    private static final String FRANKFURTER_DEV_RATE_URL = "https://api.frankfurter.dev/v2/rate";

    /** Frankfurter v1 latest 公开汇率接口地址；作为 v2 单币对接口不可用时的兼容兜底来源。 */
    private static final String FRANKFURTER_APP_LATEST_URL = "https://api.frankfurter.app/latest";

    /** JDK 内置 HTTP 客户端，避免引入额外网络请求依赖。 */
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(EXCHANGE_RATE_TIMEOUT)
            .build();

    /**
     * 字段 dictionaryRepository：保存 dictionaryRepository 对应的业务数据、运行配置或依赖对象，供本类逻辑读取和维护。
     */
    private final BasicDictionaryRepository dictionaryRepository;

    /**
     * 构造 CurrencyService 实例。
     * 
     * <p>实现步骤：
     * 1. 接收调用方传入的依赖对象或初始化参数；
     * 2. 保存到成员字段，保证后续业务方法可以复用；
     * 3. 完成实例初始化。</p>
     */
    public CurrencyService(BasicDictionaryRepository dictionaryRepository) {
        this.dictionaryRepository = dictionaryRepository;
    }

    /**
     * 根据请求币种和汇率生成业务单据的币种快照。
     *
     * <p>实现步骤：
     * 1. 币种未传时默认人民币；
     * 2. 根据字典编码读取启用币种，并校验该字典属于 CURRENCY 根节点且父级链路均启用；
     * 3. 人民币汇率固定为 1，非人民币必须传入大于 0 的汇率；
     * 4. 返回标准化后的币种编码、币种名称和 8 位小数汇率快照。</p>
     */
    @Transactional(readOnly = true)
    public CurrencySnapshot snapshot(String currencyCode, String currencyName, BigDecimal exchangeRateToCny) {
        // 步骤1：前端未传币种时按人民币处理，兼容历史调用和默认业务习惯。
        String requestedCode = normalizeCode(currencyCode);
        // 变量说明：normalizedCode 保存当前步骤计算、查询或转换得到的中间结果。
        String normalizedCode = requestedCode.isBlank() ? DEFAULT_CURRENCY_CODE : requestedCode;

        // 步骤2：人民币作为系统内置默认币种，即使字典被误删也不阻塞基础记账。
        if (DEFAULT_CURRENCY_CODE.equals(normalizedCode)) {
            return new CurrencySnapshot(DEFAULT_CURRENCY_CODE, DEFAULT_CURRENCY_NAME, CNY_EXCHANGE_RATE);
        }

        // 步骤3：非人民币必须来自启用的币种字典，避免用户输入不存在的币种单位。
        BasicDictionary dictionary = requireVisibleCurrency(normalizedCode);

        // 步骤4：非人民币需要保存业务发生当时的汇率快照，后续统计不能依赖实时汇率。
        BigDecimal normalizedRate = normalizeExchangeRate(exchangeRateToCny);
        return new CurrencySnapshot(dictionary.getCode(), dictionary.getName(), normalizedRate);
    }

    /**
     * 获取指定币种兑人民币的最新公开参考汇率。
     *
     * <p>实现步骤：
     * 1. 规范化并校验币种编码；
     * 2. 人民币直接返回 1，避免无意义外部请求；
     * 3. 非人民币先校验币种字典处于启用链路；
     * 4. 调用 Frankfurter 公开汇率接口，按当前币种到 CNY 获取最新可用参考汇率；
     * 5. 汇率统一保留 8 位小数后返回给前端，同时返回汇率日期和来源，前端仍允许用户按业务凭证实际情况手工调整。</p>
     */
    @Transactional(readOnly = true)
    public ExchangeRateSnapshot currentExchangeRateToCny(String currencyCode) {
        // 步骤1：币种编码为空时无法查询外部汇率，直接按非法参数处理。
        String normalizedCode = normalizeCode(currencyCode);
        if (normalizedCode.isBlank()) {
            throw new BusinessException(ResponseCode.ILLEGAL_PARAM, "币种编码不能为空");
        }
        // 步骤2：人民币兑人民币固定为 1，保证默认币种切换时即时响应。
        if (DEFAULT_CURRENCY_CODE.equals(normalizedCode)) {
            return new ExchangeRateSnapshot(DEFAULT_CURRENCY_CODE, DEFAULT_CURRENCY_NAME, CNY_EXCHANGE_RATE,
                    DEFAULT_CURRENCY_CODE, "系统固定汇率", null);
        }
        // 步骤3：只允许查询系统字典中启用的币种，防止停用币种继续进入业务录入。
        BasicDictionary dictionary = requireVisibleCurrency(normalizedCode);
        // 步骤4-5：调用外部接口并把结果规范到系统金额精度。
        ExternalExchangeRate externalRate = fetchExternalExchangeRate(normalizedCode);
        return new ExchangeRateSnapshot(dictionary.getCode(), dictionary.getName(), externalRate.rate(),
                DEFAULT_CURRENCY_CODE, externalRate.source(), externalRate.rateDate());
    }

    /**
     * 计算金额折算人民币后的快照金额。
     *
     * <p>实现步骤：金额为空按 0 处理，先统一 8 位金额精度，再乘以汇率并四舍五入为人民币 8 位金额。</p>
     */
    public BigDecimal toCnyAmount(BigDecimal amount, CurrencySnapshot snapshot) {
        // 变量说明：value 保存当前步骤计算、查询或转换得到的中间结果。
        BigDecimal value = amount == null ? BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP) : amount.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        return value.multiply(snapshot.exchangeRateToCny()).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * 按编码读取启用币种字典。
     *
     * <p>实现步骤：先按字典编码查询，再校验该节点和所有上级均启用且属于 CURRENCY 根字典。</p>
     */
    private BasicDictionary requireVisibleCurrency(String normalizedCode) {
        return dictionaryRepository.findByCode(normalizedCode)
                .filter(this::isVisibleCurrency)
                .orElseThrow(() -> new BusinessException(ResponseCode.ILLEGAL_PARAM, "币种不存在或已停用: " + normalizedCode));
    }

    /**
     * 从 Frankfurter 获取外部最新公开参考汇率。
     *
     * <p>实现步骤：
     * 1. 优先调用 Frankfurter v2 单币对接口，直接读取 base/quote/rate/date；
     * 2. v2 不可用时兜底调用 Frankfurter v1 latest 接口；
     * 3. 使用 JDK HttpClient 发起 GET 请求；
     * 4. 统一解析汇率值、汇率日期和来源说明；
     * 5. 对网络异常、非 2xx 响应或缺失汇率统一转换为业务异常，提示前端允许手工填写。</p>
     */
    private ExternalExchangeRate fetchExternalExchangeRate(String currencyCode) {
        // 步骤1：优先使用 Frankfurter v2 单币对接口，响应中直接包含汇率日期、基础币种、目标币种和汇率值。
        try {
            return fetchFrankfurterV2Rate(currencyCode);
        } catch (RuntimeException ex) {
            // 步骤2：v2 不可用时继续尝试 legacy latest，不把一次外部失败直接暴露给业务操作。
            return fetchFrankfurterLegacyLatest(currencyCode);
        }
    }

    /**
     * 调用 Frankfurter v2 单币对接口获取参考汇率。
     *
     * <p>实现步骤：构造 /v2/rate/{base}/{quote} 查询地址，请求成功后解析响应对象中的 rate 和 date。</p>
     */
    private ExternalExchangeRate fetchFrankfurterV2Rate(String currencyCode) {
        String url = FRANKFURTER_DEV_RATE_URL
                + "/" + URLEncoder.encode(currencyCode, StandardCharsets.UTF_8)
                + "/" + DEFAULT_CURRENCY_CODE;
        // 变量说明：payload 保存 Frankfurter v2 单币对接口返回的 JSON 对象。
        JSONObject payload = JSON.parseObject(sendExchangeRateRequest(url));
        // 变量说明：rate 保存接口返回的一单位外币可兑换人民币金额。
        BigDecimal rate = payload.getBigDecimal("rate");
        if (rate == null || rate.signum() <= 0) {
            throw new BusinessException(ResponseCode.LOAD_CLIENT_ERROR, "最新参考汇率获取失败，请手工填写汇率");
        }
        return new ExternalExchangeRate(rate.setScale(MONEY_SCALE, RoundingMode.HALF_UP), payload.getString("date"),
                "Frankfurter v2 最新参考汇率");
    }

    /**
     * 调用 Frankfurter legacy latest 接口获取参考汇率。
     *
     * <p>实现步骤：构造 from/to 查询参数，请求成功后解析响应对象中的 rates.CNY。</p>
     */
    private ExternalExchangeRate fetchFrankfurterLegacyLatest(String currencyCode) {
        String url = FRANKFURTER_APP_LATEST_URL
                + "?from=" + URLEncoder.encode(currencyCode, StandardCharsets.UTF_8)
                + "&to=" + DEFAULT_CURRENCY_CODE;
        // 变量说明：payload 保存当前步骤计算、查询或转换得到的中间结果。
        JSONObject payload = JSON.parseObject(sendExchangeRateRequest(url));
        // 变量说明：rates 保存当前步骤计算、查询或转换得到的中间结果。
        JSONObject rates = payload.getJSONObject("rates");
        // 变量说明：rate 保存当前步骤计算、查询或转换得到的中间结果。
        BigDecimal rate = rates == null ? null : rates.getBigDecimal(DEFAULT_CURRENCY_CODE);
        if (rate == null || rate.signum() <= 0) {
            throw new BusinessException(ResponseCode.LOAD_CLIENT_ERROR, "最新参考汇率获取失败，请手工填写汇率");
        }
        return new ExternalExchangeRate(rate.setScale(MONEY_SCALE, RoundingMode.HALF_UP), payload.getString("date"),
                "Frankfurter legacy 最新参考汇率");
    }

    /**
     * 发送外部汇率请求。
     *
     * <p>实现步骤：设置 5 秒超时和 JSON Accept 头；非 2xx、超时或中断统一转换为业务异常。</p>
     */
    private String sendExchangeRateRequest(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(EXCHANGE_RATE_TIMEOUT)
                    .header("Accept", "application/json")
                    .GET()
                    .build();
            // 变量说明：response 保存当前步骤计算、查询或转换得到的中间结果。
            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new BusinessException(ResponseCode.LOAD_CLIENT_ERROR, "最新参考汇率获取失败，请手工填写汇率");
            }
            return response.body();
        } catch (BusinessException ex) {
            throw ex;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ResponseCode.LOAD_CLIENT_ERROR, "最新参考汇率获取失败，请手工填写汇率");
        } catch (Exception ex) {
            throw new BusinessException(ResponseCode.LOAD_CLIENT_ERROR, "最新参考汇率获取失败，请手工填写汇率");
        }
    }

    /**
     * 判断字典是否为可用于业务的启用币种。
     *
     * <p>实现步骤：从当前币种向上追溯到根字典；任一层停用则不可用，遇到 CURRENCY 根节点才算有效币种。</p>
     */
    private boolean isVisibleCurrency(BasicDictionary dictionary) {
        // 变量说明：visited 保存当前步骤计算、查询或转换得到的中间结果。
        Set<Long> visited = new HashSet<>();
        // 变量说明：cursor 保存当前步骤计算、查询或转换得到的中间结果。
        BasicDictionary cursor = dictionary;
        while (cursor != null) {
            if (!cursor.isEnabled()) {
                return false;
            }
            if (cursor.getId() != null && !visited.add(cursor.getId())) {
                return false;
            }
            if (CURRENCY_ROOT_CODE.equals(cursor.getCode())) {
                return true;
            }
            cursor = cursor.getParent();
        }
        return false;
    }

    /**
     * 规范化币种编码。
     */
    private String normalizeCode(String currencyCode) {
        return currencyCode == null ? "" : currencyCode.trim().toUpperCase(Locale.ROOT);
    }

    /**
     * 规范化非人民币汇率。
     *
     * <p>实现步骤：校验汇率不为空且大于 0，然后统一保留 8 位小数，满足常见汇率精度需要。</p>
     */
    private BigDecimal normalizeExchangeRate(BigDecimal exchangeRateToCny) {
        if (exchangeRateToCny == null || exchangeRateToCny.signum() <= 0) {
            throw new BusinessException(ResponseCode.ILLEGAL_PARAM, "非人民币币种必须填写大于 0 的汇率");
        }
        return exchangeRateToCny.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * 币种快照值对象。
     *
     * <p>保存业务发生时使用的币种编码、币种名称和汇率。</p>
     */
    public record CurrencySnapshot(
            /**
             * 记录组件 currencyCode：表示接口入参或出参中的 currencyCode 字段。
             */
            String currencyCode,
            /**
             * 记录组件 currencyName：表示接口入参或出参中的 currencyName 字段。
             */
            String currencyName,
            /**
             * 记录组件 exchangeRateToCny：表示接口入参或出参中的 exchangeRateToCny 字段。
             */
            BigDecimal exchangeRateToCny
    ) {
    }

    /**
     * 最新公开参考汇率查询结果。
     *
     * <p>返回给前端用于新增凭证等业务自动填充汇率，用户仍可按实际业务凭证修改。当前来源为日频公开参考汇率，不代表秒级实时交易价。</p>
     */
    public record ExchangeRateSnapshot(
            /**
             * 记录组件 currencyCode：表示接口入参或出参中的 currencyCode 字段。
             */
            String currencyCode,
            /**
             * 记录组件 currencyName：表示接口入参或出参中的 currencyName 字段。
             */
            String currencyName,
            /**
             * 记录组件 exchangeRateToCny：表示接口入参或出参中的 exchangeRateToCny 字段。
             */
            BigDecimal exchangeRateToCny,
            /**
             * 记录组件 quoteCurrencyCode：表示接口入参或出参中的 quoteCurrencyCode 字段。
             */
            String quoteCurrencyCode,
            /**
             * 记录组件 source：表示接口入参或出参中的 source 字段。
             */
            String source,
            /**
             * 记录组件 rateDate：表示接口入参或出参中的 rateDate 字段。
             */
            String rateDate
    ) {
    }

    /**
     * 外部接口汇率结果。
     *
     * <p>仅在服务内部使用，保存标准化后的汇率、接口返回的汇率日期和来源说明。</p>
     */
    private record ExternalExchangeRate(BigDecimal rate, String rateDate, String source) {
    }
}

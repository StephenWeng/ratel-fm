<template>
  <div class="manual">
    <section class="manual-hero">
      <div>
        <h2>Ratel FM 系统操作手册</h2>
        <p>面向日常业务用户，说明登录、数据维护、审批、财务、采购、物流、库存、应收应付和智能检索的常用操作。</p>
      </div>
      <div class="manual-version">
        <span>适用范围</span>
        <strong>财务管理 ERP</strong>
      </div>
    </section>

    <el-input v-model.trim="keyword" clearable class="manual-search" placeholder="搜索模块、按钮、状态或操作关键词" />

    <div class="manual-layout">
      <aside class="manual-nav">
        <button
          v-for="section in visibleSections"
          :key="section.id"
          type="button"
          class="manual-nav-item"
          @click="scrollToSection(section.id)"
        >
          {{ section.title }}
        </button>
      </aside>

      <main class="manual-content">
        <section v-for="section in visibleSections" :id="section.id" :key="section.id" class="manual-section">
          <header>
            <strong>{{ section.title }}</strong>
            <span>{{ section.summary }}</span>
          </header>
          <div class="manual-card-list">
            <article v-for="item in section.items" :key="item.title" class="manual-card">
              <h3>{{ item.title }}</h3>
              <p>{{ item.description }}</p>
              <ol v-if="item.steps?.length">
                <li v-for="step in item.steps" :key="step">{{ step }}</li>
              </ol>
              <div v-if="item.note" class="manual-note">{{ item.note }}</div>
            </article>
          </div>
        </section>
        <el-empty v-if="visibleSections.length === 0" description="没有匹配的手册内容" />
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'

/**
 * 手册条目结构。
 *
 * 实现步骤：
 * 1. title 保存手册条目的短标题；
 * 2. description 保存用户可直接理解的说明；
 * 3. steps 保存可执行步骤；
 * 4. note 保存状态、权限或注意事项。
 */
interface ManualItem {
  /** 手册条目标题。 */
  title: string
  /** 手册条目说明。 */
  description: string
  /** 操作步骤列表。 */
  steps?: string[]
  /** 补充说明。 */
  note?: string
}

/**
 * 手册章节结构。
 *
 * 实现步骤：按业务模块组织手册内容，便于左侧章节导航和关键词搜索复用同一份数据。
 */
interface ManualSection {
  /** 章节唯一标识，用于页面内滚动定位。 */
  id: string
  /** 章节标题。 */
  title: string
  /** 章节摘要。 */
  summary: string
  /** 章节下的操作条目。 */
  items: ManualItem[]
}

/** 用户输入的手册搜索关键词。 */
const keyword = ref('')

/**
 * 系统操作手册章节数据。
 *
 * 实现步骤：
 * 1. 按用户日常工作顺序组织章节；
 * 2. 每个章节拆成可扫描的卡片；
 * 3. 文案只描述业务操作和注意事项，不暴露技术实现细节。
 */
const manualSections: ManualSection[] = [
  {
    id: 'manual-login',
    title: '登录与账套',
    summary: '进入系统前需要选择所属公司，系统会按所属公司隔离数据。',
    items: [
      {
        title: '登录系统',
        description: '打开登录页后，先选择所属公司，再输入登录账号或身份证号和密码。',
        steps: [
          '在“所属公司”下拉框中选择本次要进入的公司。',
          '在“登录账号”中输入账号或身份证号。',
          '输入密码后点击“登录”。',
          '如果所属公司或账号已停用，系统会提示联系管理员。'
        ],
        note: '不同所属公司之间的数据隔离。同一个账号或身份证号可以在不同公司重复存在。'
      },
      {
        title: '顶部工具区',
        description: '登录后右上角提供时间、天气、主题切换、操作手册和个人信息入口。',
        steps: [
          '悬浮时间可查看公历时间和农历日期。',
          '点击主题按钮可切换浅色、深色等主题。',
          '点击“操作手册”可随时查看本手册。',
          '点击用户名可维护个人信息、修改密码或退出登录。'
        ]
      }
    ]
  },
  {
    id: 'manual-common',
    title: '通用操作',
    summary: '列表、表单、附件、流水、导出和提示语遵循统一规则。',
    items: [
      {
        title: '左侧菜单与页签',
        description: '左侧菜单默认收起以节约页面空间，鼠标悬浮后自动展开。',
        steps: [
          '点击左侧模块或页面进入对应功能。',
          '打开过的页面会出现在顶部页签中。',
          '点击页签可快速切换页面，点击“关闭全部”会保留一个可访问页面。'
        ]
      },
      {
        title: '查询、重置和导出',
        description: '各模块列表通常提供查询条件、查询按钮和重置按钮，部分统计列表提供导出。',
        steps: [
          '输入筛选条件后点击“查询”。',
          '点击“重置”清空条件并恢复默认列表。',
          '导出会按照当前搜索条件导出当前范围内的数据。'
        ]
      },
      {
        title: '新增、编辑、详情和删除',
        description: '系统会根据单据状态和权限隐藏不可用按钮，避免用户执行不允许的操作。',
        steps: [
          '点击“新增”打开表单，按必填标识录入信息。',
          '草稿或允许修改的单据才展示“编辑”。',
          '已完成、已取消、已过账等不可修改状态通常只保留“详情”或“查看流水”。',
          '删除或批量删除前请确认数据不再使用。'
        ],
        note: '输入框校验会显示在对应字段下方，鼠标移出后会立即校验长度、格式和必填项。'
      },
      {
        title: '附件与查看流水',
        description: '支持附件的业务单据可上传附件；有操作记录的业务可通过“查看流水”追踪处理过程。',
        steps: [
          '在表单底部上传附件，附件列表可预览或下载。',
          '列表中没有附件时不展示附件按钮。',
          '点击“查看流水”可查看新增、修改、状态变更、审批等操作记录。'
        ]
      },
      {
        title: '系统提示语',
        description: '成功、警告、失败和异常会用不同方式提示。',
        steps: [
          '成功提示从右下角滑出，约 10 秒后自动消失。',
          '警告提示会居中弹出，可手工关闭，也会倒计时关闭。',
          '失败或异常会居中弹出，需要点击“关闭”。'
        ]
      }
    ]
  },
  {
    id: 'manual-basic',
    title: '基础信息',
    summary: '维护系统运行所需的字典、人员、角色和菜单。',
    items: [
      {
        title: '字典管理',
        description: '字典用于维护项目、部门、岗位、所属公司、物料、供应商、币种等基础资料。',
        steps: [
          '进入“基础信息 / 字典管理”。',
          '按层级新增或编辑字典项。',
          '页面展示字典名称和级联层级，编码由后端保存，用户无需关注编码。',
          '停用字典后，新增业务中通常不能继续选择该字典项。'
        ],
        note: '所属公司字典只有 admin 用户可以维护。'
      },
      {
        title: '人员管理',
        description: '维护系统登录人员、所属公司、部门、岗位、联系方式和角色。',
        steps: [
          'admin 可以选择和修改人员所属公司。',
          '普通用户维护人员时，所属公司默认为当前登录公司并禁用。',
          '姓名、联系方式、身份证号等字段会按格式校验。',
          '给人员分配角色后，人员即可获得对应菜单和按钮权限。'
        ]
      },
      {
        title: '角色和菜单管理',
        description: '角色用于组合菜单和按钮权限，菜单用于控制页面和按钮是否可见。',
        steps: [
          '先在“菜单管理”维护模块、页面和按钮。',
          '再在“角色管理”勾选角色可访问的菜单和按钮。',
          '用户登录后只会看到自己有权限的模块和操作按钮。'
        ]
      }
    ]
  },
  {
    id: 'manual-finance',
    title: '财务管理',
    summary: '包含会计科目、凭证记账、会计期间、出纳管理和会计平台。',
    items: [
      {
        title: '会计科目',
        description: '按编码正序展示多级会计科目，新增凭证时使用树形结构选择科目。',
        steps: [
          '进入“财务管理 / 会计科目”。',
          '按科目层级维护科目名称、类别和启用状态。',
          '选择科目时页面展示完整级联名称，不展示科目代码。'
        ]
      },
      {
        title: '凭证记账',
        description: '维护记账凭证，支持在线生成联查凭证样式的图片并预览或下载。',
        steps: [
          '点击“新增凭证”，填写凭证日期、摘要、科目、借方金额和贷方金额。',
          '借贷必须平衡后才能保存。',
          '草稿凭证可以编辑；过账或作废后不能编辑。',
          '点击“生成凭证”可查看在线凭证，凭证中的科目显示完整级联科目名称。'
        ],
        note: '涉及外币的业务生成凭证时会按汇率统一折算为人民币金额。'
      },
      {
        title: '会计期间',
        description: '按年月维护期间状态，用于月结检查、结账和反结账。',
        steps: [
          '使用年月控件选择期间。',
          '创建期间后可执行结账检查。',
          '满足条件后执行结账；需要调整时按权限执行反结账。'
        ]
      },
      {
        title: '出纳管理',
        description: '记录收款、付款等资金流水，并可关联项目、往来单位、结算方式和币种。',
        steps: [
          '新增出纳流水时选择交易日期、类型、项目、往来单位、金额、币种和汇率。',
          '切换币种后系统会自动尝试带出参考汇率，无法获取时可手工填写。',
          '确认后的流水按状态控制后续操作。'
        ]
      }
    ]
  },
  {
    id: 'manual-operation',
    title: '业务管理',
    summary: '采购管理和物流管理负责采购、审批、发货、运输和送达过程。',
    items: [
      {
        title: '采购管理',
        description: '采购单从草稿开始，经过提交审批、发起采购、已收货或取消采购等状态。',
        steps: [
          '点击“新增采购单”，填写供应商、项目、采购日期、组织结算信息和采购明细。',
          '草稿状态可编辑和提交审批。',
          '提交审批前系统会校验必填、长度和格式。',
          '审批同意后可点击“发起采购”。',
          '采购中点击“已收货”会弹出库存台账新增流水表单，并自动填充可带出的采购数据。'
        ],
        note: '取消采购需要选择取消类型并填写取消原因，取消后只保留详情和流水。'
      },
      {
        title: '物流管理',
        description: '物流单用于记录运输方式、承运信息、收发地址和运输日期。',
        steps: [
          '新增物流单时选择运输方式，不同运输方式会展示对应承运信息字段。',
          '草稿物流单可以编辑。',
          '状态变为运输中、已送达或已取消后，系统会按规则隐藏不可用按钮。',
          '已送达或已取消状态不能再编辑或状态确认。'
        ],
        note: '列表和流水中的承运信息会跟随运输方式变化，统一展示为“运输方式 + 承运信息”。'
      }
    ]
  },
  {
    id: 'manual-inventory',
    title: '库存台账',
    summary: '记录入库、出库、调拨和盘点流水，并统计物料库存。',
    items: [
      {
        title: '库存流水',
        description: '新增库存流水时选择物料、仓库、类型、数量和关联单据。',
        steps: [
          '入库需要选择目标仓库。',
          '出库需要选择来源仓库。',
          '调拨需要选择来源仓库和目标仓库。',
          '盘点需要选择盘点仓库。'
        ],
        note: '出库和调拨数量不能大于当前库存数量。库存数量 = 入库总数 - 出库总数 - 调拨总数。'
      },
      {
        title: '物料库存',
        description: '物料库存页按物料字典层级统计每种物料的当前库存。',
        steps: [
          '进入“库存台账”后切换到“物料库存”。',
          '按物料层级查看库存汇总。',
          '库存不足时，请先补录入库或核对历史流水。'
        ]
      }
    ]
  },
  {
    id: 'manual-arap',
    title: '应收应付',
    summary: '维护应收、应付、已收、已付，并提供收付统计。',
    items: [
      {
        title: '应收应付单据',
        description: '用于记录客户或供应商往来款项，支持项目、往来单位、金额、币种和摘要。',
        steps: [
          '新增单据时选择类型、项目、客户或供应商、金额和摘要。',
          '登记收付款时选择对应单据和本次收付金额。',
          '系统按单据累计已收、已付，并计算待收、待付。'
        ]
      },
      {
        title: '收付统计',
        description: '按项目和客户/供应商统计应付、已付、待付、应收、已收、待收。',
        steps: [
          '切换到“收付统计”页签。',
          '选择项目或客户/供应商后点击“查询”。',
          '点击“导出”可导出当前搜索条件下的结果。'
        ]
      }
    ]
  },
  {
    id: 'manual-workflow',
    title: '审批中心',
    summary: '集中处理待办、已办、发起事宜，并维护流程配置和流程定义。',
    items: [
      {
        title: '审批中心列表',
        description: '按当前用户展示待办事宜、已办事宜和发起事宜。',
        steps: [
          '待办事宜展示需要当前用户审批的流程，可点击“审批”或“流程查看”。',
          '已办事宜展示当前用户审批过的流程。',
          '发起事宜展示当前用户发起的流程。',
          '列表支持业务模块、审批标题、申请时间、发起人姓名和流程状态查询。'
        ],
        note: '下个节点审批人会显示“人员、部门、部门(岗位)”组合，悬浮后可查看匹配人员姓名和联系方式。'
      },
      {
        title: '流程查看和审批',
        description: '流程查看会展示业务表单、流程节点图和流程操作流水。',
        steps: [
          '点击“流程查看”查看流程已走节点和待审批节点。',
          '点击“审批”后先查看业务表单内容。',
          '选择同意或不同意，并填写审批意见。',
          '同意时审批意见默认填充“同意申请。”，仍可修改。'
        ]
      },
      {
        title: '流程管理和流程定义',
        description: '流程管理按功能模块绑定流程模板，流程定义维护节点和审批人范围。',
        steps: [
          '在“流程定义”设计流程节点。',
          '节点审批人可以选择指定人员、部门或部门下岗位。',
          '在“流程管理”把业务模块和功能模块绑定到流程定义。',
          '业务单据提交审批时，会按当前绑定模板生成流程实例。'
        ]
      }
    ]
  },
  {
    id: 'manual-ai-report',
    title: '智能检索与报表',
    summary: '通过 ratel 助手、智能检索、统计报表和操作日志辅助业务查询。',
    items: [
      {
        title: 'ratel 助手',
        description: '页面右下角悬浮图标可打开 ratel 助手，默认使用本地知识库回答系统内问题。',
        steps: [
          '点击右下角系统图标打开对话框。',
          '输入问题，例如“查询某采购单状态”或“这个月有多少起物流运输”。',
          '回答中出现“进入”按钮时，点击可跳转到目标模块并带上搜索条件。'
        ],
        note: '助手会尽量基于系统数据和知识库回答；没有数据依据时会提示原因和建议查询路径。'
      },
      {
        title: '智能检索',
        description: '智能检索用于跨模块查询业务数据，检索结果会屏蔽基础信息类内容。',
        steps: [
          '进入“智能检索”。',
          '输入自然语言问题。',
          '系统会先改写查询，再检索本地业务和知识库内容。'
        ]
      },
      {
        title: '统计报表和操作日志',
        description: '统计报表用于查看汇总数据，操作日志用于追踪用户操作。',
        steps: [
          '在“统计报表”查看系统提供的业务报表。',
          '在“操作日志”按操作时间、账号、部门、联系方式等条件查询日志。',
          '遇到数据异常时，可结合业务单据流水和操作日志定位处理过程。'
        ]
      }
    ]
  },
  {
    id: 'manual-faq',
    title: '常见问题',
    summary: '整理日常使用中最常见的处理方式。',
    items: [
      {
        title: '为什么看不到某个按钮',
        description: '按钮是否展示由权限和单据状态共同决定。',
        steps: [
          '先确认当前账号是否有对应按钮权限。',
          '再确认单据是否处于允许操作的状态。',
          '例如已过账凭证不能编辑，已送达物流单不能再次状态确认。'
        ]
      },
      {
        title: '为什么查询不到数据',
        description: '系统按所属公司隔离数据，同时列表搜索条件会影响结果。',
        steps: [
          '确认登录时选择的所属公司是否正确。',
          '点击“重置”清空查询条件后重新查询。',
          '确认数据是否已被取消、删除或处于其他状态。'
        ]
      },
      {
        title: '为什么系统提示异常',
        description: '系统会把网络、数据库、文件等技术问题转换成用户可理解的提示。',
        steps: [
          '按提示刷新页面、稍后重试或联系管理员。',
          '如果提示数据库或网络暂不可用，请保留当前操作场景并反馈管理员。',
          '管理员可通过服务端日志查看真实技术原因。'
        ]
      }
    ]
  }
]

/**
 * 根据关键词过滤手册章节。
 *
 * 实现步骤：
 * 1. 关键词为空时展示全部章节；
 * 2. 关键词匹配章节标题、摘要、条目标题、说明、步骤或备注；
 * 3. 返回符合条件的章节，保持原有顺序。
 */
const visibleSections = computed(() => {
  const text = keyword.value.trim().toLowerCase()
  if (!text) {
    return manualSections
  }
  return manualSections.filter((section) => {
    const haystack = [
      section.title,
      section.summary,
      ...section.items.flatMap((item) => [
        item.title,
        item.description,
        item.note || '',
        ...(item.steps || [])
      ])
    ].join(' ').toLowerCase()
    return haystack.includes(text)
  })
})

/**
 * 滚动到指定手册章节。
 *
 * 实现步骤：
 * 1. 按章节 id 查询 DOM；
 * 2. 找到章节后平滑滚动到抽屉可视区域顶部；
 * 3. 未找到时不执行任何操作，避免搜索过滤后点击失效报错。
 */
function scrollToSection(id: string) {
  document.getElementById(id)?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}
</script>

<style scoped>
.manual {
  display: grid;
  gap: 16px;
  color: var(--text-color);
}

.manual-hero {
  display: flex;
  align-items: stretch;
  justify-content: space-between;
  gap: 16px;
  padding: 18px;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  background: var(--subtle-surface-color);
}

.manual-hero h2 {
  margin: 0;
  color: var(--heading-color);
  font-size: 20px;
}

.manual-hero p {
  max-width: 620px;
  margin: 8px 0 0;
  color: var(--secondary-text-color);
  line-height: 1.7;
}

.manual-version {
  display: grid;
  place-content: center;
  gap: 4px;
  min-width: 150px;
  padding: 12px;
  border: 1px solid var(--soft-border-color);
  border-radius: 6px;
  background: var(--surface-color);
  text-align: center;
}

.manual-version span {
  color: var(--muted-text-color);
  font-size: 12px;
}

.manual-version strong {
  color: var(--heading-color);
}

.manual-search {
  max-width: 420px;
}

.manual-layout {
  display: grid;
  grid-template-columns: 184px minmax(0, 1fr);
  gap: 18px;
  align-items: start;
}

.manual-nav {
  position: sticky;
  top: 0;
  display: grid;
  gap: 6px;
  max-height: calc(100vh - 220px);
  overflow: auto;
  padding-right: 6px;
}

.manual-nav-item {
  min-height: 34px;
  padding: 0 10px;
  border: 1px solid var(--border-color);
  border-radius: 6px;
  background: var(--surface-color);
  color: var(--text-color);
  cursor: pointer;
  text-align: left;
}

.manual-nav-item:hover {
  border-color: var(--primary-color);
  color: var(--primary-color);
}

.manual-content {
  display: grid;
  gap: 18px;
  min-width: 0;
}

.manual-section {
  scroll-margin-top: 12px;
}

.manual-section header {
  display: grid;
  gap: 4px;
  margin-bottom: 10px;
}

.manual-section header strong {
  color: var(--heading-color);
  font-size: 18px;
}

.manual-section header span {
  color: var(--secondary-text-color);
  line-height: 1.6;
}

.manual-card-list {
  display: grid;
  gap: 10px;
}

.manual-card {
  padding: 14px;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  background: var(--surface-color);
}

.manual-card h3 {
  margin: 0;
  color: var(--heading-color);
  font-size: 15px;
}

.manual-card p {
  margin: 8px 0 0;
  color: var(--secondary-text-color);
  line-height: 1.7;
}

.manual-card ol {
  margin: 10px 0 0;
  padding-left: 20px;
  color: var(--text-color);
  line-height: 1.8;
}

.manual-note {
  margin-top: 10px;
  padding: 10px 12px;
  border: 1px solid var(--warning-border-color);
  border-radius: 6px;
  background: var(--warning-surface-color);
  color: var(--warning-color);
  line-height: 1.6;
}

@media (max-width: 760px) {
  .manual-hero,
  .manual-layout {
    grid-template-columns: 1fr;
  }

  .manual-hero {
    display: grid;
  }

  .manual-nav {
    position: static;
    grid-template-columns: repeat(auto-fit, minmax(120px, 1fr));
    max-height: none;
  }
}
</style>

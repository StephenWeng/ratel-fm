package com.ratel.fm.domain.knowledge;

/**
 * 本地知识库资料入库状态。
 *
 * <p>实现目的：区分资料上传后未处理、正在解析建索引、已经可检索和入库失败四种状态，前端据此展示进度和失败原因。</p>
 */
public enum LocalKnowledgeDocumentStatus {
    /** 文件已保存，尚未开始解析和写入向量库。 */
    PENDING,
    /** 文件正在解析、OCR、切片或写入向量库。 */
    INDEXING,
    /** 文件已完成文本解析和知识分片写入，可被智能检索和 ratel 助手引用。 */
    INDEXED,
    /** 文件解析、OCR、embedding 或向量库写入失败，需要查看 errorMessage 后重试。 */
    FAILED
}

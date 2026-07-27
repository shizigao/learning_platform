/**
 * 学习资料模块。
 *
 * <p>由分类、资料元数据、Markdown 正文、文件、站内资料引用、发布审核和访问控制
 * 构成。文件实际内容保存在 MinIO，数据库保存对象名及元数据；公开、付费和班级
 * 发放模式统一通过 {@code ContentAccessService} 判定访问权。</p>
 */
package com.learningplatform.content;

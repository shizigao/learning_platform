/**
 * 学习行为模块。
 *
 * <p>负责学习进度、浏览记录、点赞、收藏和评论。进度按 0–100 整数保存并允许用户
 * 根据当前阅读位置更新；互动写入使用唯一约束或状态切换保证重复请求安全。</p>
 
 * <!-- FILE_INDEX_START -->
 * <h2>文件职责索引</h2>
 * <ul>
 *   <li>{@code domain/ContentComment.java}：表示学习资料评论领域对象或组件，封装该概念相关的数据和行为。</li>
 *   <li>{@code domain/LearningProgress.java}：表示学习进度领域对象或组件，封装该概念相关的数据和行为。</li>
 *   <li>{@code dto/ContentCommentResponse.java}：定义学习资料评论响应接口的只读返回契约，避免直接暴露数据库实体。</li>
 *   <li>{@code dto/ContentReactionResponse.java}：定义学习资料Reaction响应接口的只读返回契约，避免直接暴露数据库实体。</li>
 *   <li>{@code dto/CreateCommentRequest.java}：定义创建评论请求接口的请求字段和 Bean Validation 约束。</li>
 *   <li>{@code dto/LearningProgressResponse.java}：定义学习进度响应接口的只读返回契约，避免直接暴露数据库实体。</li>
 *   <li>{@code dto/UpdateLearningProgressRequest.java}：定义更新学习进度请求接口的请求字段和 Bean Validation 约束。</li>
 *   <li>{@code mapper/ContentCommentMapper.java}：定义学习资料评论的 MyBatis 查询和写入操作，是业务服务访问数据库的持久化端口。</li>
 *   <li>{@code mapper/ContentInteractionMapper.java}：定义学习资料Interaction的 MyBatis 查询和写入操作，是业务服务访问数据库的持久化端口。</li>
 *   <li>{@code mapper/LearningProgressMapper.java}：定义学习进度的 MyBatis 查询和写入操作，是业务服务访问数据库的持久化端口。</li>
 *   <li>{@code package-info.java}：说明本模块总体职责、分层边界以及全部源码文件的用途。</li>
 *   <li>{@code service/ContentInteractionService.java}：实现学习资料Interaction业务规则，协调持久化组件并维护事务、权限、状态与幂等边界。</li>
 *   <li>{@code service/LearningProgressService.java}：实现学习进度业务规则，协调持久化组件并维护事务、权限、状态与幂等边界。</li>
 *   <li>{@code web/ContentInteractionController.java}：提供学习资料Interaction相关 HTTP 接口，负责请求校验、身份解析、权限入口和统一响应封装。</li>
 *   <li>{@code web/LearningProgressController.java}：提供学习进度相关 HTTP 接口，负责请求校验、身份解析、权限入口和统一响应封装。</li>
 * </ul>
 * <!-- FILE_INDEX_END -->
*/
package com.learningplatform.learning;

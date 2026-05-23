package org.chovy.canvas.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * MyBatis-Plus 自动填充 createdAt/updatedAt。
 *
 * <p>前提：实体字段名保持 `createdAt` / `updatedAt` 命名约定。
 */
@Component
public class MyMetaObjectHandler implements MetaObjectHandler {

    /** INSERT 时自动填充 createdAt/updatedAt。 */
    @Override
    public void insertFill(MetaObject metaObject) {
        // strictInsertFill 仅在字段为空时填充，避免覆盖显式传值
        this.strictInsertFill(metaObject, "createdAt", LocalDateTime.class, LocalDateTime.now());
        this.strictInsertFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());
    }

    /** UPDATE 时自动刷新 updatedAt。 */
    @Override
    public void updateFill(MetaObject metaObject) {
        // 每次更新都刷新更新时间，便于审计
        this.strictUpdateFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());
    }

    // 若实体字段名与 createdAt/updatedAt 不一致，需要同步调整此处填充键名。
}

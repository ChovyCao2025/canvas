package org.chovy.canvas.canvas.adapter.persistence;

import org.chovy.canvas.canvas.application.UserInputFormRepository;
import org.chovy.canvas.canvas.domain.UserInputForm;
import org.springframework.stereotype.Repository;

/**
 * 封装MybatisUserInputFormRepository相关的业务逻辑。
 */
@Repository
public class MybatisUserInputFormRepository implements UserInputFormRepository {

    /**
     * 保存映射器。
     */
    private final UserInputFormMapper mapper;

    /**
     * 创建当前对象实例。
     */
    public MybatisUserInputFormRepository(UserInputFormMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * 保存。
     */
    @Override
    public UserInputForm save(UserInputForm form) {
        UserInputFormDO row = UserInputPersistenceMapper.toFormRow(form);
        if (row.getId() == null) {
            int inserted = mapper.insert(row);
            if (inserted <= 0) {
                throw new IllegalStateException("User input form insert affected 0 rows");
            }
        } else {
            int updated = mapper.updateById(row);
            if (updated <= 0) {
                throw new IllegalStateException("User input form update affected 0 rows: " + row.getId());
            }
        }
        return UserInputPersistenceMapper.toFormDomain(row);
    }
}

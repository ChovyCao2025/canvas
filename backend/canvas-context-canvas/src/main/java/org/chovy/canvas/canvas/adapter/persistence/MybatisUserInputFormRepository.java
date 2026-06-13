package org.chovy.canvas.canvas.adapter.persistence;

import org.chovy.canvas.canvas.application.UserInputFormRepository;
import org.chovy.canvas.canvas.domain.UserInputForm;
import org.springframework.stereotype.Repository;

@Repository
public class MybatisUserInputFormRepository implements UserInputFormRepository {

    private final UserInputFormMapper mapper;

    public MybatisUserInputFormRepository(UserInputFormMapper mapper) {
        this.mapper = mapper;
    }

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

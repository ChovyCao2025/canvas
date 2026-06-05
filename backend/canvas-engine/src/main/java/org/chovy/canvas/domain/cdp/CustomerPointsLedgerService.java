package org.chovy.canvas.domain.cdp;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.chovy.canvas.dal.dataobject.CustomerPointsLedgerDO;
import org.chovy.canvas.dal.mapper.CustomerPointsLedgerMapper;
import org.springframework.stereotype.Service;

/**
 * Domain boundary for customer points ledger persistence.
 */
@Service
public class CustomerPointsLedgerService {

    private final CustomerPointsLedgerMapper ledgerMapper;

    public CustomerPointsLedgerService(CustomerPointsLedgerMapper ledgerMapper) {
        this.ledgerMapper = ledgerMapper;
    }

    public CustomerPointsLedgerDO findByIdempotencyKey(String idempotencyKey) {
        return ledgerMapper.selectOne(new LambdaQueryWrapper<CustomerPointsLedgerDO>()
                .eq(CustomerPointsLedgerDO::getIdempotencyKey, idempotencyKey)
                .last("LIMIT 1"));
    }

    public void insert(CustomerPointsLedgerDO ledger) {
        ledgerMapper.insert(ledger);
    }
}

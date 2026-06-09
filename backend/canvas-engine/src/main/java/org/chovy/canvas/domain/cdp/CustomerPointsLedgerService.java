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

    /**
     * 创建 CustomerPointsLedgerService 实例并注入 domain.cdp 场景依赖。
     * @param ledgerMapper 依赖组件，用于完成数据访问或外部能力调用。
     */
    public CustomerPointsLedgerService(CustomerPointsLedgerMapper ledgerMapper) {
        this.ledgerMapper = ledgerMapper;
    }

    /**
     * 执行业务操作 findByIdempotencyKey，作为CDP 客户数据的服务入口。
     * <p>该方法不接收显式租户参数时，会依赖输入对象、密钥或已有记录携带的租户信息维持隔离。
     * @param idempotencyKey 业务键，用于定位租户内的配置、资产或治理对象
     * @return 返回租户范围内的最新业务视图或持久化对象快照
     */
    public CustomerPointsLedgerDO findByIdempotencyKey(String idempotencyKey) {
        return ledgerMapper.selectOne(new LambdaQueryWrapper<CustomerPointsLedgerDO>()
                .eq(CustomerPointsLedgerDO::getIdempotencyKey, idempotencyKey)
                .last("LIMIT 1"));
    }

    /**
     * 插入业务记录，作为CDP 客户数据的服务入口。
     * <p>该方法不接收显式租户参数时，会依赖输入对象、密钥或已有记录携带的租户信息维持隔离。
     * 会通过 Mapper 写入、更新或关闭持久化记录。
     * @param ledger ledger 参数，用于 insert 流程中的校验、计算或对象转换。
     */
    public void insert(CustomerPointsLedgerDO ledger) {
        ledgerMapper.insert(ledger);
    }
}

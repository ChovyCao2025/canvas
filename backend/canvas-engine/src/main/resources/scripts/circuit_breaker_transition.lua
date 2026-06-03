-- Atomic Redis-backed circuit breaker transition.
-- KEYS[1] = circuit breaker hash key, e.g. cb:<serviceKey>
-- ARGV[1] = action: CHECK | FAILURE | SUCCESS | READ
-- ARGV[2] = failure threshold
-- ARGV[3] = open duration in milliseconds
-- ARGV[4] = half-open allowed attempts
-- ARGV[5] = current timestamp in milliseconds
-- ARGV[6] = Pub/Sub channel
--
-- Return format: <state>|<allowed 1/0>|<changed 1/0>

local key = KEYS[1]
local action = ARGV[1]
local failure_threshold = tonumber(ARGV[2])
local open_duration_ms = tonumber(ARGV[3])
local half_open_attempts = tonumber(ARGV[4])
local now = tonumber(ARGV[5])
local channel = ARGV[6]

local state = redis.call('HGET', key, 'state')
if not state then
    state = 'CLOSED'
end

local function publish(new_state)
    redis.call('PUBLISH', channel, key .. ':' .. new_state)
end

local function set_state(new_state)
    if state ~= new_state then
        state = new_state
        redis.call('HSET', key, 'state', new_state)
        publish(new_state)
        return 1
    end
    redis.call('HSET', key, 'state', new_state)
    return 0
end

if action == 'READ' then
    return state .. '|1|0'
end

if action == 'SUCCESS' then
    local failures = tonumber(redis.call('HGET', key, 'failures') or '0')
    local half_tries = tonumber(redis.call('HGET', key, 'half_tries') or '0')
    local opened_at = tonumber(redis.call('HGET', key, 'opened_at') or '0')
    local changed = 0
    if state ~= 'CLOSED' or failures ~= 0 or half_tries ~= 0 or opened_at ~= 0 then
        changed = 1
    end
    state = 'CLOSED'
    redis.call('HSET', key,
            'state', 'CLOSED',
            'failures', '0',
            'opened_at', '0',
            'half_tries', '0')
    if changed == 1 then
        publish('CLOSED')
    end
    return 'CLOSED|1|' .. changed
end

if action == 'CHECK' then
    local changed = 0
    if state == 'OPEN' then
        local opened_at = tonumber(redis.call('HGET', key, 'opened_at') or '0')
        if now - opened_at >= open_duration_ms then
            state = 'HALF_OPEN'
            redis.call('HSET', key,
                    'state', 'HALF_OPEN',
                    'half_tries', '0')
            publish('HALF_OPEN')
            changed = 1
        else
            return 'OPEN|0|0'
        end
    end

    if state == 'HALF_OPEN' then
        local half_tries = tonumber(redis.call('HINCRBY', key, 'half_tries', 1))
        if half_tries > half_open_attempts then
            return 'HALF_OPEN|0|' .. changed
        end
    end

    return state .. '|1|' .. changed
end

if action == 'FAILURE' then
    if state == 'OPEN' then
        return 'OPEN|1|0'
    end

    if state == 'HALF_OPEN' then
        redis.call('HSET', key,
                'state', 'OPEN',
                'failures', failure_threshold,
                'opened_at', now,
                'half_tries', '0')
        publish('OPEN')
        return 'OPEN|1|1'
    end

    local failures = tonumber(redis.call('HINCRBY', key, 'failures', 1))
    if failures >= failure_threshold then
        local changed = set_state('OPEN')
        redis.call('HSET', key,
                'opened_at', now,
                'half_tries', '0')
        return 'OPEN|1|' .. changed
    end

    redis.call('HSET', key, 'state', 'CLOSED')
    return 'CLOSED|1|0'
end

return state .. '|1|0'

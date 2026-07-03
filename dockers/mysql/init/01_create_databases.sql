-- ================================================================
-- 01_create_databases.sql
-- 创建所有业务数据库 + Nacos + Seata + SkyWalking + XXL-JOB 数据库
-- ================================================================

-- 业务数据库
CREATE DATABASE IF NOT EXISTS trade_user    DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
CREATE DATABASE IF NOT EXISTS trade_order   DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
CREATE DATABASE IF NOT EXISTS trade_trade   DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
CREATE DATABASE IF NOT EXISTS trade_account DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
CREATE DATABASE IF NOT EXISTS trade_product DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
CREATE DATABASE IF NOT EXISTS trade_seckill DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

-- 中间件数据库
CREATE DATABASE IF NOT EXISTS nacos_config  DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
CREATE DATABASE IF NOT EXISTS seata         DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
CREATE DATABASE IF NOT EXISTS skywalking    DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
CREATE DATABASE IF NOT EXISTS xxl_job       DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

SELECT 'All databases created!' AS status;

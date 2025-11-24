-- 创建数据库
CREATE DATABASE IF NOT EXISTS douniu DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE douniu;

-- 用户表
CREATE TABLE IF NOT EXISTS `user` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    `phone` VARCHAR(11) NOT NULL UNIQUE COMMENT '手机号',
    `password` VARCHAR(255) NOT NULL COMMENT '密码（加密）',
    `nickname` VARCHAR(50) DEFAULT NULL COMMENT '昵称',
    `avatar` VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
    `balance` INT DEFAULT 0 COMMENT '积分余额（全局，累加/减）',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX `idx_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 房间表
CREATE TABLE IF NOT EXISTS `room` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '房间ID',
    `room_code` VARCHAR(32) NOT NULL UNIQUE COMMENT '房间号（UUID去掉横杆）',
    `creator_id` BIGINT NOT NULL COMMENT '创建者ID',
    `admin_id` BIGINT NOT NULL COMMENT '管理员ID',
    `max_rounds` INT DEFAULT 20 COMMENT '总局数',
    `current_round` INT DEFAULT 0 COMMENT '当前局数',
    `enabled_card_types` VARCHAR(255) DEFAULT NULL COMMENT '启用的牌型（JSON数组）',
    `status` TINYINT DEFAULT 0 COMMENT '状态：0-等待中，1-游戏中，2-已结束',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX `idx_room_code` (`room_code`),
    INDEX `idx_creator_id` (`creator_id`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='房间表';

-- 房间玩家表
CREATE TABLE IF NOT EXISTS `room_player` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    `room_id` BIGINT NOT NULL COMMENT '房间ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `seat_number` INT NOT NULL COMMENT '座位号（1-10）',
    `is_dealer` TINYINT DEFAULT 0 COMMENT '是否庄家：0-否，1-是',
    `total_score` INT DEFAULT 0 COMMENT '当前房间总积分（每局开始时重置为0）',
    `joined_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
    UNIQUE KEY `uk_room_user` (`room_id`, `user_id`),
    UNIQUE KEY `uk_room_seat` (`room_id`, `seat_number`),
    INDEX `idx_room_id` (`room_id`),
    INDEX `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='房间玩家表';

-- 对局记录表
CREATE TABLE IF NOT EXISTS `game_record` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '对局ID',
    `room_id` BIGINT NOT NULL COMMENT '房间ID',
    `round_number` INT NOT NULL COMMENT '局数',
    `dealer_id` BIGINT NOT NULL COMMENT '庄家ID',
    `status` TINYINT DEFAULT 0 COMMENT '状态：0-进行中，1-已结算',
    `start_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '开始时间',
    `end_time` DATETIME DEFAULT NULL COMMENT '结束时间',
    INDEX `idx_room_id` (`room_id`),
    INDEX `idx_dealer_id` (`dealer_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='对局记录表';

-- 对局详情表
CREATE TABLE IF NOT EXISTS `game_detail` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    `game_record_id` BIGINT NOT NULL COMMENT '对局ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `seat_number` INT NOT NULL COMMENT '座位号',
    `bet_amount` INT NOT NULL COMMENT '投注额',
    `cards` VARCHAR(50) NOT NULL COMMENT '牌面（JSON字符串）',
    `card_type` VARCHAR(20) NOT NULL COMMENT '牌型',
    `multiplier` INT NOT NULL COMMENT '赔率',
    `score_change` INT NOT NULL COMMENT '积分变化',
    `is_winner` TINYINT DEFAULT 0 COMMENT '是否获胜：0-否，1-是',
    INDEX `idx_game_record_id` (`game_record_id`),
    INDEX `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='对局详情表';


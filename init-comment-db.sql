-- ============================================================
-- comment-service init script
-- Derived from CommentDO.java + CommentDAO.xml
-- ASCII only (no Chinese) to avoid PowerShell/docker encoding issues
-- ============================================================

CREATE DATABASE IF NOT EXISTS `comment`
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_general_ci;

USE `comment`;

-- ------------------------------------------------------------
-- Table: comment
--   Columns from CommentDO:
--     id / refId / userId / content / parentId / gmtCreated / gmtModified
--   parent_id = 0 means top-level comment, > 0 means reply
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `comment` (
    `id`           BIGINT        NOT NULL AUTO_INCREMENT COMMENT 'pk',
    `ref_id`       VARCHAR(64)   NOT NULL                COMMENT 'business id, e.g. article id',
    `user_id`      BIGINT        NOT NULL                COMMENT 'author user id',
    `content`      VARCHAR(1024) NOT NULL                COMMENT 'comment content',
    `parent_id`    BIGINT        NOT NULL DEFAULT 0      COMMENT 'parent comment id, 0=top level',
    `gmt_created`  DATETIME      NOT NULL                COMMENT 'created time',
    `gmt_modified` DATETIME      NOT NULL                COMMENT 'modified time',
    PRIMARY KEY (`id`),
    KEY `idx_ref_id`  (`ref_id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'comment table';

-- ------------------------------------------------------------
-- NOTE: there is NO user table in this database.
--   Author info belongs to user-service (user_db.user) and is
--   fetched at runtime via Feign: POST /user/ids
--   See user-service/init-user-db.sql
-- ------------------------------------------------------------

-- ------------------------------------------------------------
-- Seed data
--   user_id refers to user-service's user.id (1=zhangsan, 2=lisi, 3=wangwu)
-- ------------------------------------------------------------
INSERT INTO `comment` (`ref_id`, `user_id`, `content`, `parent_id`, `gmt_created`, `gmt_modified`) VALUES
    ('article-001', 1, 'Very clear, learned a lot!',          0, NOW(), NOW()),
    ('article-001', 2, 'Agree, especially section 3.',        1, NOW(), NOW()),
    ('article-001', 3, 'One question: why not use Redis?',    0, NOW(), NOW()),
    ('article-002', 2, 'Good post, bookmarked.',              0, NOW(), NOW());

-- ------------------------------------------------------------
-- Run (PowerShell, use cmd redirect to avoid encoding issues):
--   cmd /c "docker exec -i mysql-comment mysql -uroot -p%DB_PASSWORD% < init-comment-db.sql"
--
-- Verify:
--   docker exec mysql-comment mysql -uroot -p%DB_PASSWORD% -e "use comment; show tables; select id,user_id,content from comment;"
-- ------------------------------------------------------------

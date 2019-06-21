/*
SQLyog Enterprise v12.08 (32 bit)
MySQL - 8.0.11 : Database - distx
*********************************************************************
*/

/*!40101 SET NAMES utf8 */;

/*!40101 SET SQL_MODE=''*/;

/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;
CREATE DATABASE /*!32312 IF NOT EXISTS*/`distx` /*!40100 DEFAULT CHARACTER SET utf8 */;

USE `distx`;

/*Table structure for table `distx_app_node` */

DROP TABLE IF EXISTS `distx_app_node`;

CREATE TABLE `distx_app_node` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `app_name` varchar(100) NOT NULL COMMENT '应用名',
  `hash_code` bigint(11) NOT NULL COMMENT 'hash编码',
  `ip` varchar(20) NOT NULL,
  `last_heartbeat_time` datetime NOT NULL COMMENT '上次心跳时间',
  `status` tinyint(4) NOT NULL COMMENT '1：在线，0：不在线',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_app_ip` (`app_name`,`ip`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

/*Table structure for table `distx_task` */

DROP TABLE IF EXISTS `distx_task`;

CREATE TABLE `distx_task` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `app_name` varchar(100) NOT NULL COMMENT 'app名称，区分不同应用',
  `tx_id` varchar(50) NOT NULL COMMENT '事务id',
  `task_detail` blob NOT NULL COMMENT '任务详情',
  `hash_code` bigint(11) NOT NULL COMMENT 'hash值',
  `status` tinyint(4) NOT NULL COMMENT ' 1:创建，2：消费中，3：成功，4：失败',
  `version` tinyint(4) NOT NULL COMMENT '版本号',
  `try_times` tinyint(4) NOT NULL COMMENT '尝试次数',
  `consumer_time` datetime DEFAULT NULL COMMENT '消费时间',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_app_hash` (`app_name`,`hash_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

/*Table structure for table `test` */

-- DROP TABLE IF EXISTS `test`;
--
-- CREATE TABLE `test` (
--   `id` int(11) NOT NULL AUTO_INCREMENT,
--   `name` varchar(50) NOT NULL,
--   PRIMARY KEY (`id`)
-- ) ENGINE=InnoDB AUTO_INCREMENT=29 DEFAULT CHARSET=utf8;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

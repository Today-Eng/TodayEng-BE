-- 운영 DB의 2026-09-07 schema-only dump를 기준으로 한 Flyway baseline.
-- 기존 운영 DB는 이 파일을 실행하지 않고 version 1로 baseline 처리한다.

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `auth_account` (
  `auth_account_id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `provider` enum('GOOGLE','TEST') COLLATE utf8mb4_unicode_ci NOT NULL,
  `provider_subject` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`auth_account_id`),
  UNIQUE KEY `uk_auth_account_provider_subject` (`provider`,`provider_subject`),
  KEY `FKodq2wghn30g9soimyc2hlesyy` (`user_id`),
  CONSTRAINT `FKodq2wghn30g9soimyc2hlesyy` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `daily_context_snapshot` (
  `daily_context_snapshot_id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `collection_status` enum('FAILED','IN_PROGRESS','SUCCEEDED') COLLATE utf8mb4_unicode_ci NOT NULL,
  `context_data` json DEFAULT NULL,
  `context_date` date NOT NULL,
  `context_type` enum('CALENDAR','DIARY_MEMORY','MEMO','PHOTO','SPOTIFY','WEATHER') COLLATE utf8mb4_unicode_ci NOT NULL,
  `lease_version` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`daily_context_snapshot_id`),
  UNIQUE KEY `uk_daily_context_snapshot_user_date_type` (`user_id`,`context_date`,`context_type`),
  CONSTRAINT `FKdhtqwlhk2by1gqx5btehtv6el` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `default_question` (
  `default_question_id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `is_active` bit(1) NOT NULL,
  `korean_translation` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `question_code` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `question_text` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `question_type` enum('FOLLOW_UP','MAIN') COLLATE utf8mb4_unicode_ci NOT NULL,
  `interest_tag_id` bigint DEFAULT NULL,
  PRIMARY KEY (`default_question_id`),
  UNIQUE KEY `uk_default_question_code` (`question_code`),
  KEY `FKrpudq4cuowfipwsj6gyj6ck8y` (`interest_tag_id`),
  CONSTRAINT `FKrpudq4cuowfipwsj6gyj6ck8y` FOREIGN KEY (`interest_tag_id`) REFERENCES `interest_tag` (`interest_tag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `diary` (
  `diary_id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `completed_at` datetime(6) DEFAULT NULL,
  `context_collection_claimed_at` datetime(6) DEFAULT NULL,
  `context_collection_lease_version` bigint NOT NULL DEFAULT '0',
  `context_collection_status` enum('COLLECTING','COMPLETED','FAILED','NOT_STARTED') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'NOT_STARTED',
  `diary_date` date NOT NULL,
  `memo` varchar(2000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `question_generation_claimed_at` datetime(6) DEFAULT NULL,
  `question_generation_lease_version` bigint NOT NULL DEFAULT '0',
  `question_generation_status` enum('COMPLETED','FAILED','GENERATING','NOT_STARTED') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'NOT_STARTED',
  `status` enum('COMPLETED','DELETED','IN_PROGRESS') COLLATE utf8mb4_unicode_ci NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`diary_id`),
  UNIQUE KEY `uk_diary_user_date` (`user_id`,`diary_date`),
  KEY `idx_diary_user_status_date` (`user_id`,`status`,`diary_date`),
  CONSTRAINT `fk_diary_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `diary_answer` (
  `answer_id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `alternative_expression` json DEFAULT NULL,
  `audio_key` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `corrected_text` text COLLATE utf8mb4_unicode_ci,
  `correction_error` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `correction_reason` text COLLATE utf8mb4_unicode_ci,
  `correction_status` enum('FAILED','PENDING','PROCESSING','SUCCEEDED') COLLATE utf8mb4_unicode_ci NOT NULL,
  `original_text` text COLLATE utf8mb4_unicode_ci,
  `transcription_error` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `transcription_status` enum('FAILED','PROCESSING','SUCCEEDED','UPLOADED') COLLATE utf8mb4_unicode_ci NOT NULL,
  `question_id` bigint NOT NULL,
  PRIMARY KEY (`answer_id`),
  UNIQUE KEY `UKiv5ibv4vigs736ncc86c186ow` (`question_id`),
  CONSTRAINT `FKljesit63onx40by6kxpbyvhoe` FOREIGN KEY (`question_id`) REFERENCES `diary_question` (`question_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `diary_context` (
  `context_id` bigint NOT NULL AUTO_INCREMENT,
  `context_data` json DEFAULT NULL,
  `context_key` int NOT NULL,
  `context_type` enum('CALENDAR','DIARY_MEMORY','MEMO','PHOTO','SPOTIFY','WEATHER') COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `is_success` bit(1) NOT NULL,
  `diary_id` bigint NOT NULL,
  PRIMARY KEY (`context_id`),
  UNIQUE KEY `uk_diary_context_diary_type_key` (`diary_id`,`context_type`,`context_key`),
  CONSTRAINT `FK7eoukjywjb732pad23e4ttqge` FOREIGN KEY (`diary_id`) REFERENCES `diary` (`diary_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `diary_context_source` (
  `diary_context_source_id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `context_id` bigint NOT NULL,
  `source_diary_id` bigint NOT NULL,
  PRIMARY KEY (`diary_context_source_id`),
  UNIQUE KEY `uk_diary_context_source_context_source_diary` (`context_id`,`source_diary_id`),
  KEY `FKqfdf4bicnlbkwo5a7mnj1x91f` (`source_diary_id`),
  CONSTRAINT `FK815dgg0l6boka6ud9jx9f89d2` FOREIGN KEY (`context_id`) REFERENCES `diary_context` (`context_id`),
  CONSTRAINT `FKqfdf4bicnlbkwo5a7mnj1x91f` FOREIGN KEY (`source_diary_id`) REFERENCES `diary` (`diary_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `diary_question` (
  `question_id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `english_level_snapshot` enum('ADVANCED','BEGINNER','INTERMEDIATE') COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `generation_type` enum('AI','DEFAULT') COLLATE utf8mb4_unicode_ci NOT NULL,
  `interest_snapshot` json DEFAULT NULL,
  `keyword` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `korean_translation` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `question_order` int NOT NULL,
  `question_text` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `question_type` enum('FOLLOW_UP','MAIN') COLLATE utf8mb4_unicode_ci NOT NULL,
  `tts_audio_key` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `tts_error_message` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `tts_status` enum('FAILED','PENDING','PROCESSING','SUCCEEDED') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PENDING',
  `context_id` bigint DEFAULT NULL,
  `default_question_id` bigint DEFAULT NULL,
  `diary_id` bigint NOT NULL,
  `parent_question_id` bigint DEFAULT NULL,
  PRIMARY KEY (`question_id`),
  UNIQUE KEY `uk_diary_question_diary_type_order` (`diary_id`,`question_type`,`question_order`),
  KEY `FK5fhcvp3kt0mf5cu21u9bl405c` (`context_id`),
  KEY `FKq1imggktcbumn851m28dh94r6` (`default_question_id`),
  KEY `FK6amr2o80ne3r8dcnum4yqfhs2` (`parent_question_id`),
  CONSTRAINT `FK5fhcvp3kt0mf5cu21u9bl405c` FOREIGN KEY (`context_id`) REFERENCES `diary_context` (`context_id`),
  CONSTRAINT `FK6amr2o80ne3r8dcnum4yqfhs2` FOREIGN KEY (`parent_question_id`) REFERENCES `diary_question` (`question_id`),
  CONSTRAINT `FKq1imggktcbumn851m28dh94r6` FOREIGN KEY (`default_question_id`) REFERENCES `default_question` (`default_question_id`),
  CONSTRAINT `FKs5y4qp1jwg32wmf5ulkx5aral` FOREIGN KEY (`diary_id`) REFERENCES `diary` (`diary_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `external_account` (
  `external_account_id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `access_token` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `account_identifier` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `connected_at` datetime(6) NOT NULL,
  `provider` enum('GOOGLE_CALENDAR','SPOTIFY') COLLATE utf8mb4_unicode_ci NOT NULL,
  `provider_account_id` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `refresh_token` text COLLATE utf8mb4_unicode_ci,
  `token_expires_at` datetime(6) DEFAULT NULL,
  `use_enabled` bit(1) NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`external_account_id`),
  UNIQUE KEY `uk_external_account_user_provider` (`user_id`,`provider`),
  UNIQUE KEY `uk_external_account_provider_account` (`provider`,`provider_account_id`),
  CONSTRAINT `fk_external_account_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `interest_tag` (
  `interest_tag_id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `tag_name` enum('ART_DESIGN','BROADCAST','CAR','CARTOON_ANIMATION','CELEBRITY','COOKING_RECIPE','CULTURE_BOOK','DAILY_THOUGHT','DOMESTIC_TRAVEL','DRAMA','FASHION_BEAUTY','GAME','GARDENING','GOOD_WRITING_IMAGE','HOBBY','INTERIOR_DIY','INTERNATIONAL_TRAVEL','MOVIE','MUSIC','PARENTING_MARRIAGE','PERFORMANCE_EXHIBITION','PET','PHOTOGRAPHY','PRODUCT_REVIEW','RESTAURANT','SPORTS') COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`interest_tag_id`),
  UNIQUE KEY `uk_interest_tag_name` (`tag_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `notification_setting` (
  `notification_setting_id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `auth_key` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `p256dh_key` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `push_endpoint` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `use_enabled` bit(1) NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`notification_setting_id`),
  UNIQUE KEY `uk_notification_setting_user` (`user_id`),
  CONSTRAINT `fk_notification_setting_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `oauth_authorization_request` (
  `oauth_authorization_request_id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `completed_at` datetime(6) DEFAULT NULL,
  `expires_at` datetime(6) NOT NULL,
  `failure_stage` enum('ACCOUNT_SAVE','CALLBACK_VALIDATION','STALE_PROCESSING','TOKEN_EXCHANGE','USER_INFO') COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `failure_type` enum('AUTHORIZATION_CODE_MISSING','AUTHORIZATION_DENIED','CONFLICT','DATABASE','EXTERNAL_API','INTERNAL') COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `processing_started_at` datetime(6) DEFAULT NULL,
  `provider` enum('GOOGLE_CALENDAR','SPOTIFY') COLLATE utf8mb4_unicode_ci NOT NULL,
  `state_hash` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` enum('FAILED','ISSUED','LEGACY','PROCESSING','SUCCEEDED') COLLATE utf8mb4_unicode_ci NOT NULL,
  `used_at` datetime(6) DEFAULT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`oauth_authorization_request_id`),
  UNIQUE KEY `uk_oauth_authorization_request_state_hash` (`state_hash`),
  KEY `idx_oauth_authorization_request_expires_at` (`expires_at`),
  KEY `idx_oauth_authorization_request_status_processing` (`status`,`processing_started_at`),
  KEY `fk_oauth_authorization_request_user` (`user_id`),
  CONSTRAINT `fk_oauth_authorization_request_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `refresh_token` (
  `refresh_token_id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `expires_at` datetime(6) NOT NULL,
  `jti` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `session_id` varchar(36) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`refresh_token_id`),
  UNIQUE KEY `idx_refresh_token_jti` (`jti`),
  UNIQUE KEY `idx_refresh_token_session_id` (`session_id`),
  KEY `FKjtx87i0jvq2svedphegvdwcuy` (`user_id`),
  CONSTRAINT `FKjtx87i0jvq2svedphegvdwcuy` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `shedlock` (
  `name` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `lock_until` timestamp(3) NOT NULL,
  `locked_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `locked_by` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `terms` (
  `term_id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `active` bit(1) NOT NULL,
  `content` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `term` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `terms_type` enum('AGE_OVER_FOURTEEN','AI_PROCESSING_AND_OVERSEAS_TRANSFER','CALENDAR_INFORMATION_COLLECTION','LOCATION_INFORMATION_COLLECTION','MARKETING_INFORMATION_RECEIVE','PHOTO_EXIF_LOCATION_COLLECTION','PRIVACY_COLLECTION','SERVICE_USE','SPOTIFY_INFORMATION_COLLECTION') COLLATE utf8mb4_unicode_ci NOT NULL,
  `version` int NOT NULL,
  PRIMARY KEY (`term_id`),
  UNIQUE KEY `uk_terms_type_version` (`terms_type`,`version`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_interest` (
  `user_interest_id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `interest_tag_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`user_interest_id`),
  UNIQUE KEY `uk_user_interest_user_tag` (`user_id`,`interest_tag_id`),
  KEY `fk_user_interest_tag` (`interest_tag_id`),
  CONSTRAINT `fk_user_interest_tag` FOREIGN KEY (`interest_tag_id`) REFERENCES `interest_tag` (`interest_tag_id`),
  CONSTRAINT `fk_user_interest_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_terms` (
  `user_term_id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `agree` bit(1) NOT NULL,
  `agreed_at` datetime(6) DEFAULT NULL,
  `term_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`user_term_id`),
  UNIQUE KEY `uk_user_terms_user_term` (`user_id`,`term_id`),
  KEY `fk_user_terms_term` (`term_id`),
  CONSTRAINT `fk_user_terms_term` FOREIGN KEY (`term_id`) REFERENCES `terms` (`term_id`),
  CONSTRAINT `fk_user_terms_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `users` (
  `user_id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `current_streak` int NOT NULL,
  `email` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `english_level` enum('ADVANCED','BEGINNER','INTERMEDIATE') COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `nickname` varchar(30) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `profile_url` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `total_diary_count` bigint NOT NULL,
  `uuid` binary(16) NOT NULL,
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `uk_users_uuid` (`uuid`),
  UNIQUE KEY `uk_users_nickname` (`nickname`),
  UNIQUE KEY `uk_users_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;


-- V44: API definitions can request journey environment payload blocks.

ALTER TABLE `api_definition`
  ADD COLUMN `include_context_payload` TINYINT NOT NULL DEFAULT 0
  COMMENT '是否携带旅程环境信息，1=携带，0=不携带'
  AFTER `response_schema`;

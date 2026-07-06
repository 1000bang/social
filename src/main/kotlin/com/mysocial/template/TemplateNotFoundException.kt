package com.mysocial.template

class TemplateNotFoundException(templateId: Long) : RuntimeException("템플릿을 찾을 수 없습니다: $templateId")

package com.lexora.service.core.model

data class Organization(
    val id: String,
    val name: String,
    val isActive: Boolean,
    val branchIds: Set<String> = emptySet(),
)

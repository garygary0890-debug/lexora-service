package com.lexora.service.core.data

import com.lexora.service.core.model.Organization
import com.lexora.service.core.model.ServiceUser

interface OrganizationRepository {
    fun organizations(): List<Organization>
    fun activeOrganization(): Organization?
    fun setActiveOrganization(id: String)
}

interface UserRepository {
    fun currentUser(): ServiceUser
}

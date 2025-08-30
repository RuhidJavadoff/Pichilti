package com.ruhidjavadoff.pichilti

/**
 * Tək bir istifadəçinin bütün məlumatlarını saxlayan model.
 */
data class User(
    val id: String, // Hər istifadəçinin təkrarlanmaz ID-si
    val username: String,
    var role: UserRole, // İstifadəçinin rolu (Admin, Moderator, User)
    var permissions: Set<Permission> // İstifadəçinin sahib olduğu xüsusi icazələr
)
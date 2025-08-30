package com.ruhidjavadoff.pichilti

/**
 * Adminin moderatorlara verə biləcəyi xüsusi icazələri təyin edir.
 */
enum class Permission {
    CAN_DELETE_MESSAGES, // Mesajları silə bilər
    CAN_BAN_USERS,       // İstifadəçiləri ban edə bilər
    CAN_EDIT_PROFILES,   // Profilləri redaktə edə bilər
    CAN_APPROVE_POSTS    // Paylaşımları təsdiqləyə bilər
    // Gələcəkdə bura istədiyiniz qədər yeni icazə əlavə edə bilərsiniz
}
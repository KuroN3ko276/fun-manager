package com.funmanager.domain

import com.funmanager.data.Category
import com.funmanager.data.TransactionType

enum class CategoryNameError { EMPTY, DUPLICATE }

/** Kiểm tra tên danh mục: không rỗng, không trùng (không phân biệt hoa thường) với danh mục cùng loại khác. */
fun validateCategoryName(name: String, editing: Category?, existing: List<Category>, type: TransactionType): CategoryNameError? {
    val trimmed = name.trim()
    if (trimmed.isEmpty()) return CategoryNameError.EMPTY
    val duplicate = existing.any {
        it.type == type && it.id != editing?.id && it.name.trim().equals(trimmed, ignoreCase = true)
    }
    return if (duplicate) CategoryNameError.DUPLICATE else null
}

/** Không cho xoá danh mục cuối cùng của một loại (thu hoặc chi). */
fun canDeleteCategory(category: Category, all: List<Category>): Boolean =
    all.count { it.type == category.type } > 1

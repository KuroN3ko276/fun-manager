package com.funmanager.domain

import com.funmanager.data.Category
import com.funmanager.data.TransactionType.EXPENSE
import com.funmanager.data.TransactionType.INCOME
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CategoryRulesTest {
    private val food = Category(1, "Ăn uống", "food", 0, EXPENSE)
    private val other = Category(2, "Khác", "other", 0, EXPENSE)
    private val salary = Category(3, "Lương", "salary", 0, INCOME)
    private val all = listOf(food, other, salary)

    @Test
    fun rejectsEmptyName() {
        assertEquals(CategoryNameError.EMPTY, validateCategoryName("   ", null, all, EXPENSE))
    }

    @Test
    fun rejectsDuplicateOfSameTypeIgnoringCase() {
        assertEquals(CategoryNameError.DUPLICATE, validateCategoryName(" ăn UỐNG ", null, all, EXPENSE))
    }

    @Test
    fun allowsSameNameInOtherTypeOrWhenEditingItself() {
        assertNull(validateCategoryName("Lương", null, all, EXPENSE))
        assertNull(validateCategoryName("Ăn uống", food, all, EXPENSE))
    }

    @Test
    fun cannotDeleteLastCategoryOfType() {
        assertTrue(canDeleteCategory(food, all))
        assertFalse(canDeleteCategory(salary, all))
    }
}

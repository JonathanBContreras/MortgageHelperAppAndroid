package com.example.mortgagehelperapp

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText

/** Group the integer part while preserving decimals and the caret's logical position. */
object AmountFormatting {
    fun group(value: String): String {
        val raw = value.replace(",", "")
        if (!raw.matches(Regex("-?\\d*(\\.\\d*)?"))) return value
        val sign = if (raw.startsWith("-")) "-" else ""
        val unsigned = raw.removePrefix("-")
        val integer = unsigned.substringBefore('.')
        val decimal = if (unsigned.contains('.')) "." + unsigned.substringAfter('.') else ""
        return sign + integer.reversed().chunked(3).joinToString(",").reversed() + decimal
    }

    fun attach(field: EditText) {
        val keyboardType = field.inputType
        // Android's numeric key listener otherwise strips generated grouping commas.
        field.keyListener = android.text.method.DigitsKeyListener.getInstance("0123456789.,-")
        field.setRawInputType(keyboardType)
        field.addTextChangedListener(object : TextWatcher {
            private var updating = false
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(value: Editable?) {
                if (updating || value == null) return
                val original = value.toString()
                val formatted = group(original)
                if (original == formatted) return
                val logicalPosition = original.take(field.selectionStart.coerceAtLeast(0)).count { it != ',' }
                updating = true
                value.replace(0, value.length, formatted)
                var cursor = 0
                var count = 0
                while (cursor < formatted.length && count < logicalPosition) {
                    if (formatted[cursor] != ',') count++
                    cursor++
                }
                field.setSelection(cursor.coerceAtMost(value.length))
                updating = false
            }
        })
        field.setText(group(field.text.toString()))
    }
}

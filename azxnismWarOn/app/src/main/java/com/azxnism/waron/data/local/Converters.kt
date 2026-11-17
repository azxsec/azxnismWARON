package com.azxnism.waron.data.local

import androidx.room.TypeConverter
import com.azxnism.waron.data.model.ScanStatus
import com.azxnism.waron.data.model.Severity
import com.azxnism.waron.data.model.VulnCategory

class Converters {
    @TypeConverter
    fun fromStringList(value: List<String>): String = value.joinToString(",")

    @TypeConverter
    fun toStringList(value: String): List<String> =
        if (value.isEmpty()) emptyList() else value.split(",")

    @TypeConverter
    fun fromScanStatus(status: ScanStatus): String = status.name

    @TypeConverter
    fun toScanStatus(value: String): ScanStatus = ScanStatus.valueOf(value)

    @TypeConverter
    fun fromSeverity(severity: Severity): String = severity.name

    @TypeConverter
    fun toSeverity(value: String): Severity = Severity.valueOf(value)

    @TypeConverter
    fun fromVulnCategory(category: VulnCategory): String = category.name

    @TypeConverter
    fun toVulnCategory(value: String): VulnCategory = VulnCategory.valueOf(value)
}

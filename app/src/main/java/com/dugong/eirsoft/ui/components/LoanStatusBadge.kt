package com.dugong.eirsoft.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.dugong.eirsoft.data.model.LoanStatus
import com.dugong.eirsoft.ui.theme.*

@Composable
fun LoanStatusBadge(status: LoanStatus) {
    val containerColor = when (status) {
        LoanStatus.PENDING -> StatusPending
        LoanStatus.APPROVED -> StatusApproved
        LoanStatus.REJECTED -> StatusRejected
        LoanStatus.RETURNED -> StatusReturned
    }

    Surface(
        color = containerColor,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp),
        modifier = Modifier.padding(2.dp)
    ) {
        Text(
            text = status.name,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            color = Color.White, // Contrast is good for these specific colors in both modes
            style = MaterialTheme.typography.labelSmall
        )
    }
}

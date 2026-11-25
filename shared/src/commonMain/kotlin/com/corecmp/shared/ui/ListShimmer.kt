package com.corecmp.shared.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private const val RowCardPartnerWeight = 0.38f
private const val RowCardMetricWeight = 0.206f

@Composable
fun DetailCardShimmerList(
    modifier: Modifier = Modifier,
    itemCount: Int = 7,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        repeat(itemCount) {
            DetailCardShimmerRow()
        }
    }
}

@Composable
fun DetailCardShimmerRow() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(8.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFEAF3FB))
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ShimmerCircle(size = 52.dp)
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ShimmerLine(widthFraction = 0.72f, height = 16.dp)
                    ShimmerLine(widthFraction = 0.48f, height = 12.dp)
                }
            }
            ShimmerCircle(size = 36.dp)
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    ShimmerLine(widthFraction = 0.55f, height = 10.dp)
                    ShimmerLine(widthFraction = 0.8f, height = 12.dp)
                }
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    ShimmerLine(widthFraction = 0.5f, height = 10.dp)
                    ShimmerLine(widthFraction = 0.65f, height = 12.dp)
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    ShimmerLine(widthFraction = 0.45f, height = 10.dp)
                    ShimmerLine(widthFraction = 0.6f, height = 12.dp)
                }
                ShimmerBox(
                    modifier = Modifier.size(width = 88.dp, height = 32.dp),
                    shape = RoundedCornerShape(6.dp),
                )
            }
        }
    }
}

@Composable
fun RowCardShimmerList(
    modifier: Modifier = Modifier,
    itemCount: Int = 10,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        repeat(itemCount) {
            RowCardShimmerRow()
        }
    }
}

@Composable
private fun RowCardShimmerRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFFF8FAFC))
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(RowCardPartnerWeight),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ShimmerCircle(size = 36.dp)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ShimmerLine(widthFraction = 0.78f, height = 14.dp)
                ShimmerLine(widthFraction = 0.48f, height = 12.dp)
            }
        }
        repeat(3) {
            ShimmerBox(
                modifier = Modifier
                    .weight(RowCardMetricWeight)
                    .padding(horizontal = 4.dp)
                    .height(14.dp),
            )
        }
    }
}

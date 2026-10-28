package com.example.dormcheck

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.dormcheck.ui.DormCheckApp
import com.example.dormcheck.ui.theme.DormCheckTheme

/**
 * 单 Activity 架构：所有页面由 Compose 绘制，
 * 点名状态保存在 DormViewModel 中，屏幕旋转 / Tab 切换不会丢失。
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            DormCheckTheme {
                DormCheckApp()
            }
        }
    }
}

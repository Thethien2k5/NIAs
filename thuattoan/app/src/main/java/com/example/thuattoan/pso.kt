//package com.example.thuattoan
//
//import androidx.compose.foundation.Canvas
//import androidx.compose.foundation.background
//import androidx.compose.foundation.gestures.detectTapGestures
//import androidx.compose.foundation.layout.*
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.geometry.Offset
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.input.pointer.pointerInput
//import androidx.compose.ui.layout.onSizeChanged
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.unit.IntSize
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.unit.sp
//import kotlinx.coroutines.isActive
//import kotlin.math.pow
//import kotlin.math.sqrt
//import kotlin.random.Random
//
//// ===== PSO CONFIGURATION (Cấu hình cuối cùng cho hiệu ứng bầy đàn tự nhiên) =====
//object PSOConfig {
//    const val PARTICLE_COUNT = 150
//    const val C1 = 0.05f                 // Lực hút về kinh nghiệm cá nhân (pBest)
//    const val C2 = 0.1f                  // Lực hút về mục tiêu chung (gBest)
//    const val MAX_VELOCITY = 15f         // Vận tốc tối đa
//    const val PARTICLE_SIZE = 70f        // Kích thước hạt
//
//    // ✨ QUY TẮC MỚI: SỰ TÁCH BIỆT (SEPARATION) ✨
//    const val SEPARATION_DISTANCE = 50f  // Khoảng cách mà các hạt bắt đầu đẩy nhau ra
//    const val SEPARATION_FORCE = 0.5f    // Độ mạnh của lực đẩy
//}
//
//// ===== PARTICLE CLASS - Lớp đại diện cho một hạt =====
//data class Particle(
//    val id: Int,
//    var position: Offset,
//    var velocity: Offset = Offset(
//        x = (Random.nextFloat() - 0.5f) * 5f,
//        y = (Random.nextFloat() - 0.5f) * 5f
//    )
//) {
//    /**
//     * Cập nhật vận tốc VỚI LOGIC BẦY ĐÀN HOÀN CHỈNH
//     */
//    fun updateVelocity(
//        globalBestPosition: Offset,
//        otherParticles: List<Particle>
//    ) {
//        // Lực hút về mục tiêu chung
//        val socialForce = (globalBestPosition - position) * PSOConfig.C2 * Random.nextFloat()
//
//        // ✨ TÍNH TOÁN LỰC ĐẨY TÁCH BIỆT ✨
//        var separationForce = Offset.Zero
//        var neighborsCount = 0
//        for (other in otherParticles) {
//            if (other.id != this.id) {
//                val distance = (position - other.position).getDistance()
//                if (distance > 0 && distance < PSOConfig.SEPARATION_DISTANCE) {
//                    // Tính vector đẩy ra xa khỏi hàng xóm
//                    val pushVector = position - other.position
//                    separationForce += pushVector / (distance * distance) // Lực đẩy mạnh hơn khi ở gần
//                    neighborsCount++
//                }
//            }
//        }
//        if (neighborsCount > 0) {
//            separationForce /= neighborsCount.toFloat()
//            separationForce *= PSOConfig.SEPARATION_FORCE
//        }
//        // Kết hợp các lực
//        // Vận tốc mới = Vận tốc cũ + Lực hút + Lực đẩy
//        var newVelocity = velocity + socialForce + separationForce
//
//        // Giới hạn tốc độ
//        if (newVelocity.getDistance() > PSOConfig.MAX_VELOCITY) {
//            newVelocity = newVelocity.normalize() * PSOConfig.MAX_VELOCITY
//        }
//        velocity = newVelocity
//    }
//
//    // Cập nhật vị trí dựa trên vận tốc
//    fun updatePosition(maxWidth: Float, maxHeight: Float) {
//        position += velocity
//        // Nảy lại khi chạm biên
//        if (position.x < 0 || position.x > maxWidth) {
//            velocity = Offset(-velocity.x, velocity.y)
//            position = position.copy(x = position.x.coerceIn(0f, maxWidth))
//        }
//        if (position.y < 0 || position.y > maxHeight) {
//            velocity = Offset(velocity.x, -velocity.y)
//            position = position.copy(y = position.y.coerceIn(0f, maxHeight))
//        }
//    }
//}
//
//// ===== SWARM CLASS - Lớp quản lý bầy đàn =====
//class Swarm(private val width: Float, private val height: Float) {
//    val particles = MutableList(PSOConfig.PARTICLE_COUNT) { id ->
//        Particle(
//            id = id,
//            position = Offset(
//                x = Random.nextFloat() * width,
//                y = Random.nextFloat() * height
//            )
//        )
//    }
//
//    // gBest chính là mục tiêu người dùng chọn.
//    var globalBestPosition by mutableStateOf(Offset(width / 2, height / 2))
//        private set
//
//    var iteration = 0
//
//    // Đặt mục tiêu mới cho bầy đàn
//    fun setTarget(newTarget: Offset) {
//        globalBestPosition = newTarget
//    }
//
//    // Vòng lặp cập nhật chính
//    fun update() {
//        // Tạo một bản sao của danh sách hạt để tránh lỗi khi duyệt và sửa đổi
//        val currentParticles = particles.toList()
//        particles.forEach { particle ->
//            // Cập nhật vận tốc, truyền vào cả danh sách để tính toán lực đẩy
//            particle.updateVelocity(globalBestPosition, currentParticles)
//            // Di chuyển
//            particle.updatePosition(width, height)
//        }
//        iteration++
//    }
//}
//
//// ===== MAIN APP =====
//@Composable
//fun PSOApp() {
//    MaterialTheme {
//        Surface(modifier = Modifier.fillMaxSize()) {
//            PSOVisualization()
//        }
//    }
//}
//
//@Composable
//fun PSOVisualization() {
//    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
//
//    val swarm = remember(canvasSize) {
//        if (canvasSize != IntSize.Zero) {
//            Swarm(canvasSize.width.toFloat(), canvasSize.height.toFloat())
//        } else {
//            null
//        }
//    }
//
//    var frameTime by remember { mutableStateOf(0L) }
//
//    LaunchedEffect(swarm) {
//        if (swarm == null) return@LaunchedEffect
//        while (isActive) {
//            withFrameNanos { newTime ->
//                swarm.update()
//                frameTime = newTime
//            }
//        }
//    }
//
//    Column(
//        modifier = Modifier
//            .fillMaxSize()
//            .background(Color(0xFF0D1117))
//    ) {
//        Box(
//            modifier = Modifier
//                .weight(1f)
//                .fillMaxWidth()
//                .onSizeChanged { canvasSize = it }
//                .pointerInput(swarm) {
//                    detectTapGestures { offset ->
//                        swarm?.setTarget(offset)
//                    }
//                }
//        ) {
//            if (swarm != null) {
//                key(frameTime) {
//                    SwarmCanvas(swarm = swarm)
//                }
//            } else {
//                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
//                    CircularProgressIndicator()
//                }
//            }
//        }
//
//        if (swarm != null) {
//            InfoPanel(swarm = swarm)
//        }
//    }
//}
//
//@Composable
//fun SwarmCanvas(swarm: Swarm) {
//    Canvas(modifier = Modifier.fillMaxSize()) {
//        val targetPos = swarm.globalBestPosition
//        // Vẽ mục tiêu
//        drawCircle(
//            color = Color(0xFF30C04F),
//            radius = 25f,
//            center = targetPos,
//            alpha = 0.5f
//        )
//        drawLine(
//            color = Color.White,
//            start = Offset(targetPos.x - 18f, targetPos.y),
//            end = Offset(targetPos.x + 18f, targetPos.y),
//            strokeWidth = 3f
//        )
//        drawLine(
//            color = Color.White,
//            start = Offset(targetPos.x, targetPos.y - 18f),
//            end = Offset(targetPos.x, targetPos.y + 18f),
//            strokeWidth = 3f
//        )
//
//        // Vẽ các hạt
//        swarm.particles.forEach { particle ->
//            // Màu sắc dựa trên tốc độ, tốc độ càng cao màu càng nóng
//            val speed = particle.velocity.getDistance()
//            val ratio = (speed / PSOConfig.MAX_VELOCITY).coerceIn(0f, 1f)
//            val color = lerp(Color(0xFF87CEEB), Color(0xFFF08080), ratio) // Xanh -> Đỏ
//            drawCircle(
//                color = color,
//                radius = PSOConfig.PARTICLE_SIZE / 2,
//                center = particle.position
//            )
//        }
//    }
//}
//
//@Composable
//fun InfoPanel(swarm: Swarm) {
//    Surface(
//        modifier = Modifier.fillMaxWidth(),
//        color = Color(0xFF161B22),
//        tonalElevation = 4.dp
//    ) {
//        Column(modifier = Modifier.padding(16.dp)) {
//            Text(
//                text = "🐝 PARTICLE SWARM OPTIMIZATION",
//                color = Color.White,
//                fontSize = 18.sp,
//                fontWeight = FontWeight.Bold
//            )
//            Text(
//                text = "Nhấp vào màn hình để đặt mục tiêu mới cho bầy đàn",
//                color = Color.Gray,
//                fontSize = 12.sp
//            )
//            Spacer(modifier = Modifier.height(16.dp))
//            Row(
//                modifier = Modifier.fillMaxWidth(),
//                horizontalArrangement = Arrangement.SpaceAround
//            ) {
//                InfoItem("Vòng lặp", "${swarm.iteration}")
//                InfoItem("Số Hạt", "${PSOConfig.PARTICLE_COUNT}")
//            }
//        }
//    }
//}
//
//@Composable
//fun InfoItem(label: String, value: String) {
//    Column(horizontalAlignment = Alignment.CenterHorizontally) {
//        Text(text = value, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
//        Text(text = label, color = Color.LightGray, fontSize = 12.sp)
//    }
//}
//
//fun Offset.normalize(): Offset {
//    val len = this.getDistance()
//    return if (len > 0) this / len else this
//}
//
//fun lerp(start: Color, stop: Color, fraction: Float): Color {
//    val a = start.alpha + (stop.alpha - start.alpha) * fraction
//    val r = start.red + (stop.red - start.red) * fraction
//    val g = start.green + (stop.green - start.green) * fraction
//    val b = start.blue + (stop.blue - start.blue) * fraction
//    return Color(red = r, green = g, blue = b, alpha = a)
//}
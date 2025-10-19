//package com.example.thuattoan
//
//import androidx.compose.foundation.Canvas
//import androidx.compose.foundation.Image
//import androidx.compose.foundation.layout.*
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.draw.alpha
//import androidx.compose.ui.geometry.Offset
//import androidx.compose.ui.geometry.Size
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.graphics.painter.Painter
//import androidx.compose.ui.layout.onSizeChanged
//import androidx.compose.ui.platform.LocalDensity
//import androidx.compose.ui.res.painterResource
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.unit.IntOffset
//import androidx.compose.ui.unit.IntSize
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.unit.sp
//import kotlinx.coroutines.delay
//import kotlin.math.abs
//import kotlin.random.Random
//
//// ===== CONFIGURATION - Cấu hình =====
//object GameConfig {
//    const val POPULATION_SIZE = 50
//    const val BIRD_X_POSITION_RATIO = 0.2f
//    const val PIPE_WIDTH = 90f
//    const val PIPE_GAP_SIZE = 220f
//    const val GRAVITY = 0.6f
//    const val JUMP_VELOCITY = -11f
//    const val BIRD_SIZE = 50f
//
//    // THAY ĐỔI: Tăng khoảng cách giữa các cột lên gấp đôi
//    const val PIPE_SPAWN_INTERVAL = 240L // Trước đây là 120L
//}
//
//// ===== NEURAL NETWORK CLASS (Không thay đổi) =====
//data class NeuralNetwork(val weights: List<Float>) {
//    companion object {
//        const val INPUT_NODES = 5
//        const val HIDDEN_NODES = 8
//        const val OUTPUT_NODES = 1
//        const val TOTAL_WEIGHTS = (INPUT_NODES * HIDDEN_NODES) + (HIDDEN_NODES * OUTPUT_NODES)
//        fun createRandom(): NeuralNetwork =
//            NeuralNetwork(List(TOTAL_WEIGHTS) { Random.nextFloat() * 2 - 1 })
//    }
//
//    fun predict(inputs: List<Float>): Float {
//        val hiddenLayerOutputs = FloatArray(HIDDEN_NODES)
//        for (i in 0 until HIDDEN_NODES) {
//            var sum = 0f
//            for (j in 0 until INPUT_NODES) {
//                sum += inputs[j] * weights[i * INPUT_NODES + j]
//            }
//            hiddenLayerOutputs[i] = sigmoid(sum)
//        }
//        var outputSum = 0f
//        val outputWeightStartIndex = INPUT_NODES * HIDDEN_NODES
//        for (i in 0 until HIDDEN_NODES) {
//            outputSum += hiddenLayerOutputs[i] * weights[outputWeightStartIndex + i]
//        }
//        return sigmoid(outputSum)
//    }
//
//    private fun sigmoid(x: Float): Float = 1f / (1f + kotlin.math.exp(-x))
//}
//
//// ===== BIRD CLASS (Không thay đổi) =====
//data class Bird(
//    val id: String,
//    var y: Float,
//    var velocity: Float = 0f,
//    var isAlive: Boolean = true,
//    var fitness: Float = 0f,
//    val brain: NeuralNetwork = NeuralNetwork.createRandom(),
//    val isElite: Boolean = false
//) {
//    fun jump() {
//        if (isAlive) velocity = GameConfig.JUMP_VELOCITY
//    }
//
//    fun update(gameHeight: Float) {
//        if (isAlive) {
//            velocity += GameConfig.GRAVITY
//            y += velocity
//            fitness += 0.1f
//            if (y < 0 || y > gameHeight) {
//                isAlive = false
//            }
//        }
//    }
//
//    fun think(pipes: List<Pipe>, gameHeight: Float, gameWidth: Float, birdXPosition: Float) {
//        if (!isAlive) return
//        val nextPipe = pipes.firstOrNull { it.x + GameConfig.PIPE_WIDTH > birdXPosition } ?: return
//        val inputs = listOf(
//            y / gameHeight,
//            velocity / 15f,
//            (nextPipe.x - birdXPosition) / gameWidth,
//            (y - nextPipe.topPipeBottom) / gameHeight,
//            (y - nextPipe.bottomPipeTop) / gameHeight
//        )
//        if (brain.predict(inputs) > 0.5f) jump()
//    }
//
//    fun checkCollision(pipes: List<Pipe>, birdXPosition: Float) {
//        if (!isAlive) return
//        val birdRect = Rect(
//            left = birdXPosition - GameConfig.BIRD_SIZE / 2,
//            top = y - GameConfig.BIRD_SIZE / 2,
//            right = birdXPosition + GameConfig.BIRD_SIZE / 2,
//            bottom = y + GameConfig.BIRD_SIZE / 2
//        )
//        for (pipe in pipes) {
//            if (pipe.collidesWith(birdRect)) {
//                isAlive = false
//                fitness = (fitness - 2f).coerceAtLeast(0f)
//                return
//            }
//        }
//    }
//}
//
//// ===== PIPE CLASS (Thêm hàm tiện ích) =====
//data class Pipe(var x: Float, val gapY: Float) {
//    val topPipeBottom: Float = gapY - GameConfig.PIPE_GAP_SIZE / 2
//    val bottomPipeTop: Float = gapY + GameConfig.PIPE_GAP_SIZE / 2
//    var passed: Boolean = false
//
//    fun collidesWith(birdRect: Rect): Boolean {
//        val pipeTopRect = Rect(x, 0f, x + GameConfig.PIPE_WIDTH, topPipeBottom)
//        val pipeBottomRect = Rect(x, bottomPipeTop, x + GameConfig.PIPE_WIDTH, Float.MAX_VALUE)
//        return birdRect.overlaps(pipeTopRect) || birdRect.overlaps(pipeBottomRect)
//    }
//}
//
//data class Rect(val left: Float, val top: Float, val right: Float, val bottom: Float) {
//    fun overlaps(other: Rect): Boolean =
//        left < other.right && right > other.left && top < other.bottom && bottom > other.top
//}
//
//// ===== GENETIC ALGORITHM CLASS (THAY ĐỔI: QUAY VỀ LOGIC GỐC) =====
//class GeneticAlgorithm {
//    companion object {
//        fun createNextGeneration(
//            deadBirds: List<Bird>,
//            generation: Int,
//            initialY: Float
//        ): List<Bird> {
//            val sortedBirds = deadBirds.sortedByDescending { it.fitness }
//
//            // Lấy ra 2 cá thể tốt nhất làm cha mẹ cho cả thế hệ, theo logic của thuật toán gốc
//            val parent1 = sortedBirds.getOrNull(0)
//                ?: return List(GameConfig.POPULATION_SIZE) {
//                    Bird(
//                        id = "${generation + 1}.${it + 1}",
//                        y = initialY
//                    )
//                } // Nếu không có chim nào, tạo thế hệ mới hoàn toàn
//            val parent2 = sortedBirds.getOrNull(1) ?: parent1 // Nếu chỉ có 1, nó tự làm cha mẹ
//
//            val newBirds = mutableListOf<Bird>()
//
//            // 1. Elitism: Giữ lại 2 cá thể tốt nhất
//            newBirds.add(
//                Bird(
//                    id = "${generation + 1}.1",
//                    brain = parent1.brain,
//                    isElite = true,
//                    y = initialY
//                )
//            )
//            newBirds.add(
//                Bird(
//                    id = "${generation + 1}.2",
//                    brain = parent2.brain,
//                    isElite = true,
//                    y = initialY
//                )
//            )
//
//            // 2. Sinh sản: Dùng 2 cha mẹ tốt nhất để tạo ra phần còn lại của quần thể
//            for (i in 2 until GameConfig.POPULATION_SIZE) {
//                val childBrain = mutate(crossover(parent1.brain, parent2.brain))
//                newBirds.add(
//                    Bird(
//                        id = "${generation + 1}.${i + 1}",
//                        brain = childBrain,
//                        y = initialY
//                    )
//                )
//            }
//            return newBirds
//        }
//
//        // Lai ghép 2 bộ não để tạo ra bộ não mới
//        private fun crossover(p1: NeuralNetwork, p2: NeuralNetwork): NeuralNetwork {
//            val newWeights = p1.weights.mapIndexed { i, w ->
//                if (Random.nextFloat() < 0.5f) w else p2.weights[i]
//            }
//            return NeuralNetwork(newWeights)
//        }
//
//        // Đột biến một bộ não với một tỉ lệ nhỏ
//        private fun mutate(brain: NeuralNetwork): NeuralNetwork {
//            val newWeights = brain.weights.map { w ->
//                // Tỉ lệ và độ mạnh đột biến theo thuật toán gốc
//                if (Random.nextFloat() < 0.2f)
//                    w + Random.nextFloat() * 0.6f - 0.3f
//                else w
//            }
//            return NeuralNetwork(newWeights)
//        }
//    }
//}
//
//// ===== MAIN GAME COMPOSABLE =====
//@Composable
//fun FlappyBirdGAApp() {
//    var gameStarted by remember { mutableStateOf(false) }
//    MaterialTheme {
//        Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF87CEEB)) {
//            if (!gameStarted) {
//                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
//                    Button(onClick = { gameStarted = true }) {
//                        Text("Bắt Đầu Huấn Luyện", style = MaterialTheme.typography.headlineMedium)
//                    }
//                }
//            } else {
//                GameScreen()
//            }
//        }
//    }
//}
//
//@Composable
//fun GameScreen() {
//    val birdPainter = painterResource(id = R.drawable.img_chim)
//
//    var generation by remember { mutableStateOf(1) }
//    var birds by remember { mutableStateOf<List<Bird>>(emptyList()) }
//    var pipes by remember { mutableStateOf<List<Pipe>>(emptyList()) }
//    var maxFitness by remember { mutableStateOf(0f) }
//    var gameAreaSize by remember { mutableStateOf(IntSize.Zero) }
//    val birdXPosition = gameAreaSize.width * GameConfig.BIRD_X_POSITION_RATIO
//
//    // Khởi tạo thế hệ đầu tiên
//    LaunchedEffect(gameAreaSize) {
//        if (gameAreaSize != IntSize.Zero && birds.isEmpty()) {
//            birds = List(GameConfig.POPULATION_SIZE) {
//                Bird(id = "1.${it + 1}", y = gameAreaSize.height / 2f)
//            }
//        }
//    }
//
//    // Game Loop
//    LaunchedEffect(generation, gameAreaSize) {
//        if (birds.isEmpty() || gameAreaSize == IntSize.Zero) return@LaunchedEffect
//
//        var frameCount = 0L
//        while (birds.any { it.isAlive }) {
//            delay(16) // ~60 FPS
//
//            birds.forEach { bird ->
//                if (bird.isAlive) {
//                    bird.think(
//                        pipes,
//                        gameAreaSize.height.toFloat(),
//                        gameAreaSize.width.toFloat(),
//                        birdXPosition
//                    )
//                    bird.update(gameAreaSize.height.toFloat())
//                    bird.checkCollision(pipes, birdXPosition)
//                    pipes.firstOrNull { !it.passed && it.x + GameConfig.PIPE_WIDTH < birdXPosition }
//                        ?.let { passedPipe ->
//                            passedPipe.passed = true
//                            bird.fitness += 50f
//                            val distanceToCenter = abs(bird.y - passedPipe.gapY)
//                            val bonus =
//                                (GameConfig.PIPE_GAP_SIZE / 2 - distanceToCenter) / (GameConfig.PIPE_GAP_SIZE / 2)
//                            bird.fitness += (bonus * 10f).coerceAtLeast(0f)
//                        }
//                }
//            }
//            pipes = pipes.map { it.copy(x = it.x - 3f) }.filter { it.x > -GameConfig.PIPE_WIDTH }
//
//            // Tạo cột mới theo khoảng thời gian đã cấu hình
//            if (frameCount % GameConfig.PIPE_SPAWN_INTERVAL == 0L) {
//                pipes += Pipe(
//                    x = gameAreaSize.width.toFloat(),
//                    gapY = Random.nextFloat() * (gameAreaSize.height - GameConfig.PIPE_GAP_SIZE - 200) + 100 + GameConfig.PIPE_GAP_SIZE / 2
//                )
//            }
//            frameCount++
//            birds = birds.toList()
//        }
//
//        maxFitness = birds.maxOfOrNull { it.fitness }?.coerceAtLeast(maxFitness) ?: maxFitness
//        birds = GeneticAlgorithm.createNextGeneration(birds, generation, gameAreaSize.height / 2f)
//        generation++
//        pipes = emptyList()
//    }
//
//    Column(modifier = Modifier.fillMaxSize()) {
//        GameCanvas(
//            modifier = Modifier
//                .weight(1f)
//                .fillMaxWidth()
//                .onSizeChanged { gameAreaSize = it },
//            birds = birds,
//            pipes = pipes,
//            birdPainter = birdPainter,
//            birdXPosition = birdXPosition
//        )
//        InfoPanel(generation, birds, maxFitness)
//    }
//}
//
//@Composable
//fun GameCanvas(
//    modifier: Modifier,
//    birds: List<Bird>,
//    pipes: List<Pipe>,
//    birdPainter: Painter,
//    birdXPosition: Float
//) {
//    Box(modifier = modifier) {
//        Canvas(modifier = Modifier.fillMaxSize()) {
//            val pipeColor = Color(0xFF2E7D32)
//            pipes.forEach { pipe ->
//                drawRect(
//                    color = pipeColor,
//                    topLeft = Offset(pipe.x, 0f),
//                    size = Size(GameConfig.PIPE_WIDTH, pipe.topPipeBottom)
//                )
//                drawRect(
//                    color = pipeColor,
//                    topLeft = Offset(pipe.x, pipe.bottomPipeTop),
//                    size = Size(GameConfig.PIPE_WIDTH, size.height - pipe.bottomPipeTop)
//                )
//            }
//        }
//
//        birds.sortedBy { it.isAlive }.forEach { bird ->
//            val birdSize = if (bird.isElite) GameConfig.BIRD_SIZE * 1.2f else GameConfig.BIRD_SIZE
//            val birdSizeDp = with(LocalDensity.current) { birdSize.toDp() }
//
//            Image(
//                painter = birdPainter,
//                contentDescription = "Bird",
//                modifier = Modifier
//                    .offset {
//                        IntOffset(
//                            x = (birdXPosition - birdSize / 2).toInt(),
//                            y = (bird.y - birdSize / 2).toInt()
//                        )
//                    }
//                    .size(birdSizeDp)
//                    .alpha(if (bird.isAlive) 1f else 0.3f)
//            )
//        }
//    }
//}
//
//@Composable
//fun InfoPanel(generation: Int, birds: List<Bird>, maxFitness: Float) {
//    Surface(modifier = Modifier.fillMaxWidth(), color = Color(0xFF1E1E1E), tonalElevation = 8.dp) {
//        Row(
//            modifier = Modifier
//                .padding(16.dp)
//                .fillMaxWidth(),
//            horizontalArrangement = Arrangement.SpaceAround,
//            verticalAlignment = Alignment.CenterVertically
//        ) {
//            InfoText(label = "Thế hệ", value = "$generation")
//            InfoText(
//                label = "Còn sống",
//                value = "${birds.count { it.isAlive }} / ${GameConfig.POPULATION_SIZE}"
//            )
//            InfoText(label = "Fitness Cao Nhất", value = "${maxFitness.toInt()}")
//        }
//    }
//}
//
//@Composable
//fun InfoText(label: String, value: String) {
//    Column(horizontalAlignment = Alignment.CenterHorizontally) {
//        Text(text = label, color = Color.LightGray, fontSize = 14.sp)
//        Text(text = value, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
//    }
//}
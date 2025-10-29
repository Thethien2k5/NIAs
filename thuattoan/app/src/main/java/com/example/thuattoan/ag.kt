package com.example.thuattoan

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.random.Random

// ===== CONFIGURATION - Cấu hình =====
// GameConfig
// Mục tiêu: Chứa các hằng số cấu hình cho mô phỏng Flappy Bird với Genetic Algorithm (GA).
// Công dụng các thuộc tính:
// - POPULATION_SIZE: số lượng cá thể (chim) trong mỗi thế hệ.
// - BIRD_X_POSITION_RATIO: tỉ lệ vị trí ngang của chim so với chiều rộng màn hình.
// - PIPE_WIDTH, PIPE_GAP_SIZE: kích thước cột và khoảng hở giữa hai cột.
// - GRAVITY, JUMP_VELOCITY: tham số vật lý mô phỏng trọng lực và lực nhảy.
// - BIRD_SIZE: kích thước hình ảnh chim (dùng để vẽ và phát hiện va chạm).
// - PIPE_SPAWN_INTERVAL: khoảng thời gian (frame) giữa các lần sinh cột mới.
object GameConfig {
    const val POPULATION_SIZE = 50
    const val BIRD_X_POSITION_RATIO = 0.2f
    const val PIPE_WIDTH = 90f
    const val PIPE_GAP_SIZE = 220f
    const val GRAVITY = 0.6f
    const val JUMP_VELOCITY = -11f
    const val BIRD_SIZE = 50f

    // THAY ĐỔI: Tăng khoảng cách giữa các cột lên gấp đôi
    const val PIPE_SPAWN_INTERVAL = 240L // Trước đây là 120L
}

// ===== NEURAL NETWORK CLASS (Không thay đổi) =====
// NeuralNetwork
// Mục tiêu: Mô phỏng một mạng nơ-ron đơn giản (feed-forward) dùng để quyết định khi nào chim nhảy.
// Công dụng thuộc tính:
// - weights: danh sách trọng số nối giữa lớp input -> hidden và hidden -> output.
// Ghi chú: Mạng này là nhỏ, cố định kích thước (INPUT_NODES, HIDDEN_NODES, OUTPUT_NODES)
// và không có bias riêng biệt; trọng số được đặt tuần tự trong một danh sách.
data class NeuralNetwork(val weights: List<Float>) {
    companion object {
        const val INPUT_NODES = 5
        const val HIDDEN_NODES = 8
        const val OUTPUT_NODES = 1
        const val TOTAL_WEIGHTS = (INPUT_NODES * HIDDEN_NODES) + (HIDDEN_NODES * OUTPUT_NODES)
        // Tạo một mạng với trọng số khởi tạo ngẫu nhiên trong khoảng [-1, 1]
        fun createRandom(): NeuralNetwork =
            NeuralNetwork(List(TOTAL_WEIGHTS) { Random.nextFloat() * 2 - 1 })
    }

    // === NeuralNetwork.predict ===
    // Mục tiêu: Tính đầu ra (0..1) của mạng cho bộ input cho trước.
    // Cách hoạt động:
    //  - Tính đầu ra lớp ẩn bằng tích ma trận (inputs * weights_input_hidden) rồi áp sigmoid.
    //  - Tính đầu ra cuối cùng bằng tích giữa hidden outputs và weights_hidden_output rồi áp sigmoid.
    // Trả về: giá trị sigmoid cuối cùng; trong ứng dụng, giá trị > 0.5 => quyết định nhảy.
    // === End highlighted description ===
    fun predict(inputs: List<Float>): Float {
        val hiddenLayerOutputs = FloatArray(HIDDEN_NODES)
        for (i in 0 until HIDDEN_NODES) {
            var sum = 0f
            for (j in 0 until INPUT_NODES) {
                sum += inputs[j] * weights[i * INPUT_NODES + j]
            }
            hiddenLayerOutputs[i] = sigmoid(sum)
        }
        var outputSum = 0f
        val outputWeightStartIndex = INPUT_NODES * HIDDEN_NODES
        for (i in 0 until HIDDEN_NODES) {
            outputSum += hiddenLayerOutputs[i] * weights[outputWeightStartIndex + i]
        }
        return sigmoid(outputSum)
    }

    // Hàm kích hoạt sigmoid chuẩn (0..1)
    private fun sigmoid(x: Float): Float = 1f / (1f + kotlin.math.exp(-x))
}

// ===== BIRD CLASS (Không thay đổi) =====
// Bird
// Mục tiêu: Lưu trạng thái của một cá thể trong quần thể (chim). Chứa cả "brain" (NeuralNetwork)
// và các thuộc tính vận động/fitness.
// Thuộc tính:
// - id: định danh chuỗi để dễ debug/ghi log.
// - y: vị trí theo trục dọc.
// - velocity: vận tốc theo trục dọc.
// - isAlive: trạng thái còn sống hay chết.
// - fitness: điểm fitness dùng để sắp xếp và chọn lọc trong GA.
// - brain: mạng nơ-ron dùng để quyết định hành vi (nhảy hay không).
// - isElite: đánh dấu cá thể được giữ lại (elitism) cho thế hệ sau.
data class Bird(
    val id: String,
    var y: Float,
    var velocity: Float = 0f,
    var isAlive: Boolean = true,
    var fitness: Float = 0f,
    val brain: NeuralNetwork = NeuralNetwork.createRandom(),
    val isElite: Boolean = false
) {
    // Make the bird perform a jump by setting its vertical velocity.
    fun jump() {
        if (isAlive) velocity = GameConfig.JUMP_VELOCITY
    }

    // Update bird physics and fitness each frame.
    fun update(gameHeight: Float) {
        if (isAlive) {
            velocity += GameConfig.GRAVITY
            y += velocity
            // Reward staying alive slightly each frame
            fitness += 0.1f
            if (y < 0 || y > gameHeight) {
                isAlive = false
            }
        }
    }

    // === Bird.think ===
    // Mục tiêu: Chuẩn bị input cho NeuralNetwork và quyết định nhảy hay không.
    // Input gồm (tỉ lệ chuẩn hóa):
    // 1) y / gameHeight: vị trí dọc của chim
    // 2) velocity / 15f: vận tốc chuẩn hóa
    // 3) (nextPipe.x - birdXPosition) / gameWidth: khoảng cách ngang tới cột tiếp theo
    // 4) (y - nextPipe.topPipeBottom) / gameHeight: độ cao so với mép dưới cột trên
    // 5) (y - nextPipe.bottomPipeTop) / gameHeight: độ cao so với mép trên cột dưới
    // Mạng trả về một giá trị sigmoid; nếu > 0.5 thì gọi jump().
    // === End highlighted description ===
    fun think(pipes: List<Pipe>, gameHeight: Float, gameWidth: Float, birdXPosition: Float) {
        if (!isAlive) return
        val nextPipe = pipes.firstOrNull { it.x + GameConfig.PIPE_WIDTH > birdXPosition } ?: return
        val inputs = listOf(
            y / gameHeight,
            velocity / 15f,
            (nextPipe.x - birdXPosition) / gameWidth,
            (y - nextPipe.topPipeBottom) / gameHeight,
            (y - nextPipe.bottomPipeTop) / gameHeight
        )
        if (brain.predict(inputs) > 0.5f) jump()
    }

    // === Bird.checkCollision ===
    // Mục tiêu: Kiểm tra va chạm giữa bird và danh sách pipes bằng phép kiểm tra chữ nhật.
    // Hậu quả: nếu va chạm, bird.isAlive = false và fitness bị trừ nhẹ.
    // === End highlighted description ===
    fun checkCollision(pipes: List<Pipe>, birdXPosition: Float) {
        if (!isAlive) return
        val birdRect = Rect(
            left = birdXPosition - GameConfig.BIRD_SIZE / 2,
            top = y - GameConfig.BIRD_SIZE / 2,
            right = birdXPosition + GameConfig.BIRD_SIZE / 2,
            bottom = y + GameConfig.BIRD_SIZE / 2
        )
        for (pipe in pipes) {
            if (pipe.collidesWith(birdRect)) {
                isAlive = false
                fitness = (fitness - 2f).coerceAtLeast(0f)
                return
            }
        }
    }
}

// ===== PIPE CLASS (Thêm hàm tiện ích) =====
// Pipe
// Mục tiêu: Đại diện cho một cặp ống có khoảng hở (gap) ở vị trí ngang x và vị trí dọc gapY.
// Thuộc tính:
// - x: tọa độ ngang (thay đổi theo thời gian khi di chuyển về bên trái).
// - gapY: tâm của khoảng hở dọc giữa 2 ống.
// - topPipeBottom / bottomPipeTop: ranh giới dưới của ống trên và trên của ống dưới.
// - passed: đánh dấu đã bị chim vượt qua (dùng để cộng điểm).
data class Pipe(var x: Float, val gapY: Float) {
    val topPipeBottom: Float = gapY - GameConfig.PIPE_GAP_SIZE / 2
    val bottomPipeTop: Float = gapY + GameConfig.PIPE_GAP_SIZE / 2
    var passed: Boolean = false

    // Kiểm tra va chạm giữa birdRect và 2 phần (trên và dưới) của ống
    fun collidesWith(birdRect: Rect): Boolean {
        val pipeTopRect = Rect(x, 0f, x + GameConfig.PIPE_WIDTH, topPipeBottom)
        val pipeBottomRect = Rect(x, bottomPipeTop, x + GameConfig.PIPE_WIDTH, Float.MAX_VALUE)
        return birdRect.overlaps(pipeTopRect) || birdRect.overlaps(pipeBottomRect)
    }
}

// Simple rectangle utility struct used for collision detection
data class Rect(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    fun overlaps(other: Rect): Boolean =
        left < other.right && right > other.left && top < other.bottom && bottom > other.top
}

// ===== GENETIC ALGORITHM CLASS  =====
// GeneticAlgorithm
// Mục tiêu: Chứa các phương thức tĩnh để tạo thế hệ tiếp theo dựa trên tập deadBirds.
// Công dụng chính: sắp xếp theo fitness, giữ elitism (giữ các cá thể hàng đầu),
// thực hiện crossover và mutation để sinh quần thể mới.
class GeneticAlgorithm {
    companion object {
        // === createNextGeneration ===
        // Mục tiêu: Từ danh sách chim đã chết (deadBirds) tạo ra danh sách chim cho thế hệ tiếp theo.
        // Quy trình:
        // 1) Sắp xếp các cá thể theo fitness giảm dần.
        // 2) Lấy 2 cá thể tốt nhất làm cha mẹ chính (parent1, parent2).
        // 3) Áp dụng elitism: giữ lại 2 cá thể tốt nhất (không mutate) vào thế hệ mới.
        // 4) Sinh các cá thể còn lại bằng cách crossover rồi mutate các trọng số.
        // Trả về: danh sách mới có kích thước GameConfig.POPULATION_SIZE.
        // === End highlighted description ===
        fun createNextGeneration(
            deadBirds: List<Bird>,
            generation: Int,
            initialY: Float
        ): List<Bird> {
            val sortedBirds = deadBirds.sortedByDescending { it.fitness }

            // Lấy ra 2 cá thể tốt nhất làm cha mẹ cho cả thế hệ, theo logic của thuật toán gốc
            val parent1 = sortedBirds.getOrNull(0)
                ?: return List(GameConfig.POPULATION_SIZE) {
                    Bird(
                        id = "${generation + 1}.${it + 1}",
                        y = initialY
                    )
                } // Nếu không có chim nào, tạo thế hệ mới hoàn toàn
            val parent2 = sortedBirds.getOrNull(1) ?: parent1 // Nếu chỉ có 1, nó tự làm cha mẹ

            val newBirds = mutableListOf<Bird>()

            // 1. Elitism: Giữ lại 2 cá thể tốt nhất
            newBirds.add(
                Bird(
                    id = "${generation + 1}.1",
                    brain = parent1.brain,
                    isElite = true,
                    y = initialY
                )
            )
            newBirds.add(
                Bird(
                    id = "${generation + 1}.2",
                    brain = parent2.brain,
                    isElite = true,
                    y = initialY
                )
            )

            // 2. Sinh sản: Dùng 2 cha mẹ tốt nhất để tạo ra phần còn lại của quần thể
            for (i in 2 until GameConfig.POPULATION_SIZE) {
                val childBrain = mutate(crossover(parent1.brain, parent2.brain))
                newBirds.add(
                    Bird(
                        id = "${generation + 1}.${i + 1}",
                        brain = childBrain,
                        y = initialY
                    )
                )
            }
            return newBirds
        }

        // === crossover ===
        // Mục tiêu: Lai ghép hai NeuralNetwork (p1, p2) bằng phương pháp chọn trọng số từ 2 cha mẹ
        // ở mỗi chỉ số với xác suất 50%.
        // Ghi chú: Đây là một dạng crossover đơn giản (uniform crossover) không dùng điểm cắt.
        // === End highlighted description ===
        private fun crossover(p1: NeuralNetwork, p2: NeuralNetwork): NeuralNetwork {
            val newWeights = p1.weights.mapIndexed { i, w ->
                if (Random.nextFloat() < 0.5f) w else p2.weights[i]
            }
            return NeuralNetwork(newWeights)
        }

        // === mutate ===
        // Mục tiêu: Áp dụng đột biến cho các trọng số của một NeuralNetwork.
        // Cơ chế: với xác suất 0.2 trên mỗi trọng số, cộng thêm một số ngẫu nhiên trong [-0.3, 0.3].
        // Ghi chú: Tỉ lệ và biên độ đột biến có thể điều chỉnh để cân bằng khám phá/khai thác.
        // === End highlighted description ===
        private fun mutate(brain: NeuralNetwork): NeuralNetwork {
            val newWeights = brain.weights.map { w ->
                // Tỉ lệ và độ mạnh đột biến theo thuật toán gốc
                if (Random.nextFloat() < 0.2f)
                    w + Random.nextFloat() * 0.6f - 0.3f
                else w
            }
            return NeuralNetwork(newWeights)
        }
    }
}

// ===== MAIN GAME COMPOSABLE =====
@Composable
fun FlappyBirdGAApp() {
    var gameStarted by remember { mutableStateOf(false) }
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF87CEEB)) {
            if (!gameStarted) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Button(onClick = { gameStarted = true }) {
                        Text("Bắt Đầu Huấn Luyện", style = MaterialTheme.typography.headlineMedium)
                    }
                }
            } else {
                GameScreen()
            }
        }
    }
}

@Composable
fun GameScreen() {
    val birdPainter = painterResource(id = R.drawable.img_chim)

    var generation by remember { mutableStateOf(1) }
    var birds by remember { mutableStateOf<List<Bird>>(emptyList()) }
    var pipes by remember { mutableStateOf<List<Pipe>>(emptyList()) }
    var maxFitness by remember { mutableStateOf(0f) }
    var gameAreaSize by remember { mutableStateOf(IntSize.Zero) }
    val birdXPosition = gameAreaSize.width * GameConfig.BIRD_X_POSITION_RATIO

    // Khởi tạo thế hệ đầu tiên
    LaunchedEffect(gameAreaSize) {
        if (gameAreaSize != IntSize.Zero && birds.isEmpty()) {
            birds = List(GameConfig.POPULATION_SIZE) {
                Bird(id = "1.${it + 1}", y = gameAreaSize.height / 2f)
            }
        }
    }

    // Game Loop
    LaunchedEffect(generation, gameAreaSize) {
        if (birds.isEmpty() || gameAreaSize == IntSize.Zero) return@LaunchedEffect

        var frameCount = 0L
        while (birds.any { it.isAlive }) {
            delay(16) // ~60 FPS

            birds.forEach { bird ->
                if (bird.isAlive) {
                    bird.think(
                        pipes,
                        gameAreaSize.height.toFloat(),
                        gameAreaSize.width.toFloat(),
                        birdXPosition
                    )
                    bird.update(gameAreaSize.height.toFloat())
                    bird.checkCollision(pipes, birdXPosition)
                    pipes.firstOrNull { !it.passed && it.x + GameConfig.PIPE_WIDTH < birdXPosition }
                        ?.let { passedPipe ->
                            passedPipe.passed = true
                            bird.fitness += 50f
                            val distanceToCenter = abs(bird.y - passedPipe.gapY)
                            val bonus =
                                (GameConfig.PIPE_GAP_SIZE / 2 - distanceToCenter) / (GameConfig.PIPE_GAP_SIZE / 2)
                            bird.fitness += (bonus * 10f).coerceAtLeast(0f)
                        }
                }
            }
            pipes = pipes.map { it.copy(x = it.x - 3f) }.filter { it.x > -GameConfig.PIPE_WIDTH }

            // Tạo cột mới theo khoảng thời gian đã cấu hình
            if (frameCount % GameConfig.PIPE_SPAWN_INTERVAL == 0L) {
                pipes += Pipe(
                    x = gameAreaSize.width.toFloat(),
                    gapY = Random.nextFloat() * (gameAreaSize.height - GameConfig.PIPE_GAP_SIZE - 200) + 100 + GameConfig.PIPE_GAP_SIZE / 2
                )
            }
            frameCount++
            birds = birds.toList()
        }

        maxFitness = birds.maxOfOrNull { it.fitness }?.coerceAtLeast(maxFitness) ?: maxFitness
        birds = GeneticAlgorithm.createNextGeneration(birds, generation, gameAreaSize.height / 2f)
        generation++
        pipes = emptyList()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        GameCanvas(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .onSizeChanged { gameAreaSize = it },
            birds = birds,
            pipes = pipes,
            birdPainter = birdPainter,
            birdXPosition = birdXPosition
        )
        InfoPanel(generation, birds, maxFitness)
    }
}

@Composable
fun GameCanvas(
    modifier: Modifier,
    birds: List<Bird>,
    pipes: List<Pipe>,
    birdPainter: Painter,
    birdXPosition: Float
) {
    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val pipeColor = Color(0xFF2E7D32)
            pipes.forEach { pipe ->
                drawRect(
                    color = pipeColor,
                    topLeft = Offset(pipe.x, 0f),
                    size = Size(GameConfig.PIPE_WIDTH, pipe.topPipeBottom)
                )
                drawRect(
                    color = pipeColor,
                    topLeft = Offset(pipe.x, pipe.bottomPipeTop),
                    size = Size(GameConfig.PIPE_WIDTH, size.height - pipe.bottomPipeTop)
                )
            }
        }

        birds.sortedBy { it.isAlive }.forEach { bird ->
            val birdSize = if (bird.isElite) GameConfig.BIRD_SIZE * 1.2f else GameConfig.BIRD_SIZE
            val birdSizeDp = with(LocalDensity.current) { birdSize.toDp() }

            Image(
                painter = birdPainter,
                contentDescription = "Bird",
                modifier = Modifier
                    .offset {
                        IntOffset(
                            x = (birdXPosition - birdSize / 2).toInt(),
                            y = (bird.y - birdSize / 2).toInt()
                        )
                    }
                    .size(birdSizeDp)
                    .alpha(if (bird.isAlive) 1f else 0.3f)
            )
        }
    }
}

@Composable
fun InfoPanel(generation: Int, birds: List<Bird>, maxFitness: Float) {
    Surface(modifier = Modifier.fillMaxWidth(), color = Color(0xFF1E1E1E), tonalElevation = 8.dp) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            InfoText(label = "Thế hệ", value = "$generation")
            InfoText(
                label = "Còn sống",
                value = "${birds.count { it.isAlive }} / ${GameConfig.POPULATION_SIZE}"
            )
            InfoText(label = "Fitness Cao Nhất", value = "${maxFitness.toInt()}")
        }
    }
}

@Composable
fun InfoText(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, color = Color.LightGray, fontSize = 14.sp)
        Text(text = value, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}
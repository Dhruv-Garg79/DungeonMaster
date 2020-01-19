package com.example.dungeonmaster

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*
import java.util.*

class MainActivity : AppCompatActivity() {

    private var n = 10 // columns
    private var m = n //rows
    private lateinit var viewGrid: Array<Array<ImageView>>
    private lateinit var dataGrid: Array<Array<Operations>>
    private lateinit var visited: Array<BooleanArray>
    private var itemW: Int = 0
    private var itemH: Int = 0
    private val dx = intArrayOf(1, 0, -1, 0)
    private val dy = intArrayOf(0, -1, 0, 1)

    private var job : Job = Job()

    private var currentOp = Operations.START
    private var start = Pair(0, 0)
    private var end = Pair(0, 0)
    private var isPlaying = false
    private var currentAlgo = Algos.BFS

    private lateinit var bottomSheetBehavior : BottomSheetBehavior<ConstraintLayout>

    private val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val listener = object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                frameLayout.viewTreeObserver.removeOnGlobalLayoutListener(this)
                initialize()
            }
        }

        frameLayout.viewTreeObserver.addOnGlobalLayoutListener(listener)
    }

    private fun initialize() {

        bottomSheetBehavior = BottomSheetBehavior.from(bottom_sheet)

        itemW = frameLayout.width / n
        itemH = itemW

        m = (n * frameLayout.height / frameLayout.width)
        end = Pair(n - 1, m - 1)

        viewGrid = Array(n) {
            Array(m) { ImageView(this) }
        }

        dataGrid = Array(n) {
            Array(m) { Operations.OPEN }
        }

        visited = Array(n) {
            BooleanArray(m) { false }
        }

        for (i in 0 until n) {
            for (j in 0 until m) {
                val imageView = viewGrid[i][j]
                imageView.layoutParams = FrameLayout.LayoutParams(itemW - 5, itemH - 5)
                imageView.setBackgroundColor(Color.WHITE)
                imageView.x = (i * itemW).toFloat()
                imageView.y = (j * itemH).toFloat()
                frameLayout.addView(imageView)
            }
        }

        setStart(start.first, start.second)
        setEnd(end.first, end.second)

        frameLayout.setOnTouchListener { _, event ->
            Log.d(TAG, "touchy")
            if (event.action == MotionEvent.ACTION_DOWN && !isPlaying) {
                val i = (event.x / itemW).toInt()
                val j = (event.y / itemH).toInt()

                Log.d(TAG, "i = $i, j = $j")

                when (currentOp) {
                    Operations.START -> setStart(i, j)
                    Operations.END -> setEnd(i, j)
                    Operations.OBSTRUCTION -> addObstruction(i, j)
                    else -> {}
                }
                true
            } else false
        }

        obsButtons.setOnClickListener {
            currentOp = Operations.OBSTRUCTION
        }
        startButton.setOnClickListener {
            currentOp = Operations.START
        }
        endButton.setOnClickListener {
            currentOp = Operations.END
        }

        radioGroup.setOnCheckedChangeListener { group, checkedId ->
            when(checkedId){
                R.id.bfs -> currentAlgo = Algos.BFS
                R.id.dfs -> currentAlgo = Algos.DFS
                R.id.djikstra -> currentAlgo = Algos.DJIKSTRA
            }
        }
    }

    private fun findPathDFS(x1: Int, y1: Int, x2: Int, y2: Int) = runBlocking {
        val visited = Array(n) {
            BooleanArray(m) { false }
        }

        val parent = Array(n) {
            Array(m) {
                Array(2) {
                    -1
                }
            }
        }


        val stackX: Stack<Int> = Stack()
        val stackY: Stack<Int> = Stack()
        stackX.add(x1)
        stackY.add(y1)
        var x = 0
        var y = 0

        isPlaying = true

        var reachedEnd = false
        var move_count = 0
        job = CoroutineScope(Dispatchers.Default).launch {
            while (!stackX.isEmpty()) {
                x = stackX.pop() ?: -1
                y = stackY.pop() ?: -1

                if (dataGrid[x][y] == Operations.END) {
                    reachedEnd = true
                    break
                }

                if (!visited[x][y]) {
                    move_count++
                    changeBgColor(x, y, Color.YELLOW)
                    visited[x][y] = true
                }


                for (i in dx.indices) {
                    val nX = x + dx[i] //neighbour of node
                    val nY = y + dy[i]
                    if (nX >= 0 && nY >= 0 && nX < n && nY < m && dataGrid[nX][nY] != Operations.OBSTRUCTION && !visited[nX][nY]) {
                        stackX.add(nX)
                        stackY.add(nY)
                        parent[nX][nY][0] = x
                        parent[nX][nY][1] = y
                    }
                }
            }

            if (!reachedEnd) withContext(Dispatchers.Main) {
                Toast.makeText(this@MainActivity, "No path b/w S and E", Toast.LENGTH_LONG).show()
            }
            else pathAnim(parent, move_count, x1, y1, x2, y2)
        }
    }

    private fun findPathDjikstra(x1: Int, y1: Int, x2: Int, y2: Int) = runBlocking {

    }

    private fun findPathBFS(x1: Int, y1: Int, x2: Int, y2: Int) = runBlocking {

        val visited = Array(n) {
            BooleanArray(m) { false }
        }

        val parent = Array<Array<Array<Int>>>(n) {
            Array(m) {
                Array(2) {
                    -1
                }
            }
        }

        var move_count = 0 // total no of moves to reach E from S
        var nodes_left_in_layer = 1 // total no of unvisited neighbours of current layer
        var nodes_in_next_layer = 0 // total no of unvisited neighbours of next layer
        var reachedEnd = false

        val queueX: Queue<Int> = LinkedList()
        val queueY: Queue<Int> = LinkedList()
        queueX.add(x1)
        queueY.add(y1)
        visited[x1][y1] = true
        var x = 0
        var y = 0

        isPlaying = true
        job = CoroutineScope(Dispatchers.Default).launch {
            Log.d(TAG, "Hi from Coroutine")

            while (!queueX.isEmpty()) {
                x = queueX.poll() ?: -1
                y = queueY.poll() ?: -1

                for (i in 0 until dx.size) {
                    val nX = x + dx[i] //neighbour of node
                    val nY = y + dy[i]
                    if (nX >= 0 && nY >= 0 && nX < n && nY < m && dataGrid[nX][nY] != Operations.OBSTRUCTION) {
                        if (!visited[nX][nY]) {

                            parent[nX][nY][0] = x
                            parent[nX][nY][1] = y
                            nodes_in_next_layer++

                            if (dataGrid[nX][nY] == Operations.END) {
                                reachedEnd = true
                                break
                            }
                            queueX.add(nX)
                            queueY.add(nY)
                            visited[nX][nY] = true
                            changeBgColor(nX, nY, Color.YELLOW)
                        }
                        else if (nX != x1 && nY != y1 && nX != x2 && nY != y2) {
                            changeBgColor(nX, nY, Color.GRAY)
                        }
                    }
                }
                // so that we only count one neighbour of cell instead of all of them i.e. only for one path
                // instead of all possible path we search
                if (--nodes_left_in_layer == 0) {
                    nodes_left_in_layer = nodes_in_next_layer
                    nodes_in_next_layer = 0
                    move_count++
                }

                if (reachedEnd) break
            }

            if (!reachedEnd) withContext(Dispatchers.Main) {
                Toast.makeText(this@MainActivity, "No path b/w S and E", Toast.LENGTH_LONG).show()
            }
            else pathAnim(parent, move_count, x1, y1, x2, y2)
        }
    }

    private suspend fun pathAnim(
        parent: Array<Array<Array<Int>>>,
        move_count: Int,
        x1: Int,
        y1: Int,
        x2: Int,
        y2: Int
    ) {
        withContext(Dispatchers.Main) {
            Toast.makeText(
                this@MainActivity,
                "The total no of steps to reach E : $move_count",
                Toast.LENGTH_LONG
            ).show()
        }

        Log.d(TAG, "Path Animation")

        var i = parent[x2][y2][0]
        var j = parent[x2][y2][1]
        while (i > -1 && j > -1) {

            Log.d(TAG, "i = $i, j = $j")

            changeBgColor(i, j, Color.BLUE)
            i = parent[i][j][0]
            j = parent[i][j][1]
            if (i == x1 && j == y1) break
        }

    }

    suspend fun changeBgColor(i: Int, j: Int, color: Int) {
        delay(75)
        viewGrid[i][j].setBackgroundColor(color)
    }

    fun addObstruction(x: Int, y: Int) {
        if (dataGrid[x][y] == Operations.OPEN) {
            dataGrid[x][y] = Operations.OBSTRUCTION
            viewGrid[x][y].setImageResource(R.drawable.ic_block_24dp)
        } else {
            dataGrid[x][y] = Operations.OPEN
            viewGrid[x][y].setImageDrawable(null)
        }
    }

    fun setStart(x: Int, y: Int) {
        // remove old start point
        dataGrid[start.first][start.second] = Operations.OPEN
        viewGrid[start.first][start.second].setImageDrawable(null)

        // set new start point
        start = Pair(x, y)
        dataGrid[start.first][start.second] = Operations.START
        viewGrid[start.first][start.second].setImageResource(R.drawable.ic_sentiment_neutral_24dp)
    }

    fun setEnd(x: Int, y: Int) {
        // remove old end point
        dataGrid[end.first][end.second] = Operations.OPEN
        viewGrid[end.first][end.second].setImageDrawable(null)

        // set new end point
        end = Pair(x, y)
        dataGrid[x][y] = Operations.END
        viewGrid[x][y].setImageResource(R.drawable.ic_sentiment_satisfied_24dp)
    }

    fun reset() = runBlocking{
        job.cancelAndJoin()
        for (i in 0 until n){
            for (j in 0 until m){
                viewGrid[i][j].setBackgroundColor(Color.WHITE)
                viewGrid[i][j].setImageDrawable(null)
                dataGrid[i][j] = Operations.OPEN
            }
        }

        isPlaying = false
        setStart(start.first, start.second)
        setEnd(end.first, end.second)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.settings -> {
                bottomSheetBehavior.state =
                    if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED) BottomSheetBehavior.STATE_COLLAPSED else BottomSheetBehavior.STATE_EXPANDED
                true
            }
            R.id.reset ->{
                reset()
                true
            }
            R.id.play -> {
                if (!isPlaying) {
                    when(currentAlgo) {
                        Algos.BFS -> findPathBFS(start.first, start.second, end.first, end.second)
                        Algos.DFS -> findPathDFS(start.first, start.second, end.first, end.second)
                        Algos.DJIKSTRA -> findPathDjikstra(start.first, start.second, end.first, end.second)
                    }
                    true
                }
                else false
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onStop() {
        super.onStop()
        job.cancel()
    }

    private enum class Operations {
        START, END, OBSTRUCTION, OPEN
    }

    private enum class Algos {
        BFS, DFS, DJIKSTRA
    }
}

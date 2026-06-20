/* =================================
    * GLOBAL STATE VARIABLES
    * =================================
    * questions: Array of quiz question objects fetched from API
    * currentIndex: Tracks current question position (0-based indexing)
    * score: Counts the number of correct answers given by the user
    * answered: Boolean flag to prevent multiple answer clicks per question
    * =================================
    */
let questions = [];
let currentIndex = 0;
let score = 0;
let answered = false;

/* =================================
    * LOAD QUESTIONS FROM API
    * Fetches quiz data from server
    * Shows loading indicator during fetch operation
    * Displays error if API is unavailable
    * ================================= */
async function loadQuestions() {
    const loadingEl = document.getElementById('loading');
    const questionScreen = document.getElementById('question-screen');
    
    // Show loading indicator and hide question screen
    loadingEl.style.display = 'block';
    questionScreen.style.display = 'none';

    try {
        // Attempt to fetch questions from server API
        const response = await fetch('/static/v0/json/sports_quiz.json', {
            method: 'GET',
            headers: { 'Accept': 'application/json' }
        });
        
        // Check if API response is successful
        if (!response.ok) throw new Error('API returned error');
        
        // Parse JSON response and store questions
        questions = await response.json();
        console.log('Questions loaded from server');
    } catch (err) {
        // Display error message if API fails
        console.log('Failed to load questions');
        loadingEl.textContent = 'Failed to load questions. Please try again later.';
        loadingEl.style.display = 'block';
        return;
    }

    // Hide loading indicator and show question screen
    loadingEl.style.display = 'none';
    questionScreen.style.display = 'block';

    // Reset game state and start the quiz
    currentIndex = 0;
    score = 0;
    showQuestion();
}

/* =================================
    * DISPLAY CURRENT QUESTION
    * Renders the current question text and answer options on the screen
    * Resets the "answered" flag to allow new answer selection
    * ================================= */
function showQuestion() {
    answered = false;
    
    const q = questions[currentIndex];
        if (!q) {
        // All questions completed, show results screen
        showResults();
        return;
    }

    // Update progress display with current question number and total count
    document.getElementById('current-q').textContent = currentIndex + 1;
    document.getElementById('total-q').textContent = questions.length;

    // Display current question text
    document.getElementById('question-text').textContent = q.question;

    // Clear existing options and generate new answer buttons dynamically
    const optionsContainer = document.getElementById('options');
    optionsContainer.innerHTML = '';

    Object.keys(q.options).forEach(key => {
        const optionDiv = document.createElement('div');
        optionDiv.className = 'option';
        optionDiv.dataset.letter = key;
        
        optionDiv.innerHTML = `
            <span class="letter">${key}</span>
            <span class="text">${q.options[key]}</span>
        `;

        optionDiv.addEventListener('click', handleAnswerClick);
        optionsContainer.appendChild(optionDiv);
    });
}

/* =================================
    * HANDLE ANSWER SELECTION
    * Processes user's answer click, validates against correct answer,
    * provides visual feedback, and schedules next question
    * ================================= */
function handleAnswerClick(e) {
    if (answered) return;
    answered = true;

    // Get the selected answer letter (A, B, C, or D)
    const selectedLetter = e.currentTarget.dataset.letter;
    const correctLetter = questions[currentIndex].correct;

    // Disable all option buttons to prevent multiple clicks
    const allOptions = document.querySelectorAll('.option');
    allOptions.forEach(opt => {
        opt.style.pointerEvents = 'none';
        opt.style.transition = 'all 0.4s';
    });

    // Highlight selected answer - correct or wrong
    const selectedBtn = e.currentTarget;
    if (selectedLetter === correctLetter) {
        // Correct answer - highlight in green and increment score
        selectedBtn.classList.add('correct');
        score++;
    } else {
        // Wrong answer - highlight in red and show correct answer
        selectedBtn.classList.add('wrong');
        
        // Find and highlight the correct answer
        const correctBtn = Array.from(allOptions).find(
            opt => opt.dataset.letter === correctLetter
        );
        if (correctBtn) correctBtn.classList.add('correct');
    }

    // Delay before moving to next question
    setTimeout(() => {
        currentIndex++;
        if (currentIndex < questions.length) {
            // Load next question
            showQuestion();
        } else {
            // All questions completed, show results
            showResults();
        }
    }, 1800);
}

/* =================================
    * DISPLAY RESULTS SCREEN
    * Calculates final score, percentage, and displays appropriate message
    * switch from question screen to results screen
    * ================================= */
function showResults() {
    document.getElementById('question-screen').style.display = 'none';
    const resultsScreen = document.getElementById('results-screen');
    resultsScreen.style.display = 'block';

    const percent = Math.round((score / questions.length) * 100);
    document.getElementById('score').textContent = `${score}/${questions.length}`;

    // Determine feedback message based on performance percentage
    // Display performance message with appropriate emoji
    if (percent === 100) message = 'legend!';
    else if (percent >= 80) message = 'nice!';
    else if (percent >= 60) message = 'not bad!';
    else message = 'try again';

    // Show the feedback message on screen
}

/* =================================
    * RESTART QUIZ
    * Resets game state, switches back to question screen, and starts over
    * ================================= */
function restartQuiz() {
    document.getElementById('results-screen').style.display = 'none';
    document.getElementById('question-screen').style.display = 'block';
    currentIndex = 0;
    score = 0;
    showQuestion();
}

/* =================================
    * INITIALIZATION
    * Starts the quiz when page finishes loading
    * ================================= */
window.onload = function() {
    loadQuestions();
};
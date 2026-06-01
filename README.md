# AI Student Predictive Analytics Dashboard & Service

A unified, professional-grade student performance analysis application. This repository contains both the Spring Boot backend service and the dynamic AI-driven dashboard.

## 🌟 Key Features
- **Combined Architecture**: The optimized frontend is embedded directly into the Spring Boot backend resources. The entire application runs on a single port (**`9091`**), eliminating CORS issues and complex hosting configurations.
- **AI-Powered Predictive Analysis**: Processes student report card documents, OCR data, and handwritten answer sheets using advanced AI vision to extract performance metrics, gauges, grades, strengths, weaknesses, and risk factors.
- **Dynamic Charting**: Rich and responsive Bar, Radar, and Doughnut data visualizations using Chart.js.
- **Handwriting Assessment**: AI-driven analysis of handwritten presentation and penmanship.

---

## 📂 Project Structure
- **`predictive-dashboard/`**: The modern, high-fidelity Vite + HTML/JS/CSS frontend.
- **`ssms-predictive-module/`**: The Spring Boot backend. The built frontend is served from `src/main/resources/static/`.

---

## 🚀 Running Locally

### 1. Unified Run (Frontend + Backend on Port 9091)
1. Go to the backend directory:
   ```bash
   cd ssms-predictive-module
   ```
2. Start the Spring Boot application (using Maven or your IDE):
   ```bash
   mvn spring-boot:run
   ```
3. Open your browser and navigate to:
   ```text
   http://localhost:9091/index.html
   ```

### 2. Frontend Development (Port 5173 with hot-reloading)
1. Go to the dashboard directory:
   ```bash
   cd predictive-dashboard
   ```
2. Install dependencies and start the Vite dev server:
   ```bash
   npm install
   npm run dev
   ```
3. The dashboard will launch on `http://localhost:5173` and automatically route API requests back to `http://localhost:9091/api/predictive`.

---

## ☁️ Deployment
This combined project is fully optimized for one-click hosting on platforms like **Railway** or **Render**:
- Root directory when linking repository: `predictive analysis/ssms-predictive-module`
- Serves both frontend and APIs under a single public URL.
- Remember to configure `PREDICTIVE_AI_API_KEY` in your environment variables!

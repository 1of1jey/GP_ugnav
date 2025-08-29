UG Campus Navigation System
📌 Project Overview

This project is a Java-based navigation system designed to help users find routes within the University of Ghana (UG) campus. It models campus roads and buildings as a graph and applies graph algorithms (such as Dijkstra’s algorithm and A* search) to compute the shortest or most optimal paths between locations.

The system provides a simple interface where users can:

Select a starting point and a destination on campus.

View the calculated route with distance or estimated travel time.

Explore alternative routes (where applicable).

⚙️ Features

Graph-based Campus Map: Campus is represented as nodes (buildings, landmarks) and edges (paths, roads).

Shortest Path Algorithms:

Dijkstra’s Algorithm → Finds the shortest path by distance.

A* Search → Provides efficient route finding using heuristics.

Custom Weights: Supports different weight modes such as:

Distance (meters)

Estimated time (minutes)

Extensible Design: New locations or roads can be added easily to the graph data.

🚀 How It Works

The Graph class loads all campus locations and roads.

The user enters a start and end point.

An algorithm (Dijkstra or A*) is run to compute the shortest path.

The result is returned as a Route object containing:

Ordered list of nodes to follow

Total distance / estimated time

The UI displays the recommended path.

▶️ Running the Project
Prerequisites

Java 11+

Maven or Gradle (for build)

Steps

Clone the repository:

git clone https://github.com/your-username/ug-campus-navigation.git
cd ug-campus-navigation


Compile the project:

mvn clean install


Run the application:

java -cp target/ug-campus-navigation.jar ui.Main

🧪 Example Usage

Input:

Start: Balme Library
Destination: Legon Hall
Algorithm: Dijkstra


Output:

Shortest Path Found:
Balme Library → Central Cafeteria → Legon Hall
Distance: 1.2 km
Estimated Time: 15 minutes

📖 Algorithms Used

Dijkstra’s Algorithm: Guarantees the shortest path by exploring nodes in order of minimum distance.

A* Search: Uses heuristics (like straight-line distance) to speed up pathfinding while still producing an optimal solution.

🌍 Future Enhancements

GUI integration with JavaFX for map visualization.

Real-time traffic or congestion data.

GPS support for mobile devices.

Multi-criteria optimization (e.g., fastest vs. safest route).

👨‍💻 Authors

[Your Name]

Binary KLV Chat System

A multi-threaded client–server chat application built in Java with a JavaFX user interface, using a custom binary Key-Length-Value (KLV) protocol for all communication.

🚀 Learning Objectives

By completing this project, you will:

✓ Implement a binary protocol directly from specification

✓ Understand byte-level data representation and endianness

✓ Build a multi-threaded server capable of handling concurrent clients

✓ Manage shared state using proper synchronization

✓ Parse and construct nested binary data structures

✓ Handle full bidirectional socket communication

✓ Debug network traffic at the protocol level

✓ Experience key differences between binary and text-based protocols

💻 What You’ll Build

The Server

A multi-threaded Java server that:

Listens on a specified port for incoming clients

Accepts and manages multiple simultaneous client connections

Parses binary KLV messages received from clients

Maintains a list of active connections

Broadcasts messages to all connected clients

Stores and retrieves the last 20 messages as message history

Handles client disconnects gracefully

The Client

A JavaFX client application that:

Connects to the server using host + port

Sends correctly formatted binary KLV messages

Parses binary server responses

Displays messages from other users in real time

Lets the user send messages and disconnect cleanly

Supports message history retrieval on connect

🛠️ Technologies Used

Java — core application logic, networking, threading

JavaFX — graphical UI for the client

Custom Binary KLV Protocol — efficient structured communication

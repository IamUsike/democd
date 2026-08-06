// Package repository handles persistence for simulation state and run history.
//
// Implementations may target an in-memory store (default / test) or a
// relational database. The service layer depends only on repository
// interfaces defined here, not on concrete implementations.
package repository


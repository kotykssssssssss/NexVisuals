package dev.nexvisuals.core.module;

/** Optional explicit editor command such as copying a hand transform. */
public record ModuleAction(String name, String description, Runnable run) { }

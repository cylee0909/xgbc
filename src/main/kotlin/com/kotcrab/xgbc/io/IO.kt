package com.kotcrab.xgbc.io

import com.kotcrab.xgbc.Emulator

/** @author Kotcrab */
class IO(private val emulator: Emulator) {
    val ioMem: ByteArray = ByteArray(0x4C)
    val devicesMap = arrayOfNulls<IODevice>(0x4C)

    val serialPort = SerialPort(emulator)
    val div = Div(emulator)
    val timer = Timer(emulator)
    val lcd = Lcd(emulator)
    val joypad = Joypad(emulator)

    val devices = arrayOf(serialPort, timer, div, lcd, joypad)

    var cyclesSynced = 0

    init {
        for (device in devices) {
            device.register({ addr -> devicesMap[addr - 0xFF00] = device })
        }
    }

    fun reset() {
        ioMem.fill(0)
        for (device in devices) {
            device.reset()
        }
        cyclesSynced = 0
    }

    fun tick(cyclesElapsed: Int) {
        for (device in devices) {
            device.tick(cyclesElapsed)
        }
    }

    fun sync(cycles: Int) {
        cyclesSynced += cycles
        tick(cycles)
    }

    fun getAndClearSyncedCycles(): Int {
        val cyclesTmp = cyclesSynced
        cyclesSynced = 0
        return cyclesTmp
    }

    fun read(addr: Int): Byte {
        val relAddr = addr - 0xFF00
        val device = devicesMap[relAddr]
        device?.onRead(addr)
        return ioMem[relAddr]
    }

    fun write(addr: Int, value: Byte) {
        val relAddr = addr - 0xFF00
        ioMem[relAddr] = value
        val device = devicesMap[relAddr]
        device?.onWrite(addr, value)
    }

    fun directWrite(addr: Int, value: Byte) {
        ioMem[addr - 0xFF00] = value
    }
}

interface IODevice {
    fun reset()

    fun tick(cyclesElapsed: Int)

    fun register(registrar: (addr: Int) -> Unit)

    fun onRead(addr: Int)

    fun onWrite(addr: Int, value: Byte)
}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.github.ilmoeuro.futurehack2016;

/**
 *
 * @author Ilmo Euro <ilmo.euro@gmail.com>
 */
@FunctionalInterface
public interface AccelerometerDataHandler {
    void onDataReceived(long time, int acceleration, int jerk);
}

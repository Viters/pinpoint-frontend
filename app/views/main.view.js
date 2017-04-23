'use strict'

import React, { Component } from 'react'
import { StyleSheet, Text, View, Button } from 'react-native'
import BeaconModule from './../native/beacon.module'

export default class MainView extends Component {
  constructor(props) {
    super(props)
  
    BeaconModule.start()

    this.state = {
      position: {
        x: 0,
        y: 0,
        error: 'false'
      },
      tries: 0
    }

    setInterval(() => this._updatePosition(), 1000)
    setInterval(() => this._getPosition(), 5000)
  }

  render() {

    return (
      <View style={styles.container}>
        <Text style={styles.welcome}>
          Pinpoint
        </Text>
        <Text style={styles.instructions}>
          Projekt, który wygra hackaton
        </Text>
        <Text style={styles.instructions}>
          Znajdziemy Ci drogę na lajcie xD
        </Text>
        <Text>X: {this.state.position.x}, Y: {this.state.position.y}</Text>
        <Text>Prób: {this.state.tries}</Text>
      </View>
    )
  }

  _updatePosition() {
    BeaconModule.updatePosition()
  }

  _getPosition() {
    BeaconModule.getPosition((position) => { 
      if (position.error != 'true') 
        this.setState({
          position
        })
      let newTries = this.state.tries + 1
      this.setState({ tries: newTries })
    }
  )}
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: '#F5FCFF',
  },
  welcome: {
    fontSize: 20,
    textAlign: 'center',
    margin: 10,
  },
  instructions: {
    textAlign: 'center',
    color: '#333333',
    marginBottom: 5,
  },
})
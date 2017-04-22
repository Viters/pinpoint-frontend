'use strict'

import React, { Component } from 'react'
import { StyleSheet, Text, View } from 'react-native'

export default class MainView extends Component {
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
      </View>
    )
  }
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
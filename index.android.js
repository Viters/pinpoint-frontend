'use strict'

import React, { Component } from 'react'
import { AppRegistry } from 'react-native'
import MainView from './app/views/main.view.js'

export default class pinpoint extends Component {
  render() {
    return(
      <MainView />
    )
  }
}

AppRegistry.registerComponent('pinpoint', () => pinpoint);

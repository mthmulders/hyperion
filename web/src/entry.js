import 'babel-polyfill';

import React from 'react';
import Router from 'react-router';
import routes from './routes.js';

const ReactDOM = require('react-dom');

require.context(
    './assets/',
    true,
    /.*/
);

require('./style.css');
require('./assets/images/favicon.png');

// 'touch' outside a focused input or textarea should hide the keyboard on iOS
document.addEventListener('touchstart', (e) => {
    const elements = ['INPUT', 'TEXTAREA'];
    if (!elements.includes(e.target.tagName) && elements.includes(document.activeElement.tagName)) {
        document.activeElement.blur();
    }
}, false);

ReactDOM.render(routes, document.getElementById('app'));

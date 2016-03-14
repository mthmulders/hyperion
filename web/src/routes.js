import React from 'react';

import { IndexRoute, Router, Route } from 'react-router';

import { browserHistory } from 'react-router';

// Import all pages
import LandingPage from 'pages/LandingPage/';
import Index from 'components/Home/';

// Mind the order of routes: first match is served
const routes = (
    <Router history={ browserHistory }>
        <Route path="/" component={ LandingPage }>
            <IndexRoute component={ Index } />
            <Route path="index" component={ Index } />
        </Route>
    </Router>
);

export default routes;

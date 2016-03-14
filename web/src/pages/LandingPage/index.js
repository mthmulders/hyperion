import React from 'react';

class LandingPage extends React.Component {
    constructor(props) {
        super(props);
        this.state = {};
    }

    render() {
        const renderChild = (child) => <li>{ child }</li>;
        return (
            <div>
                This is the 'Landing' page. It might have children, which are displayed below.
                <ul>
                    { React.Children.map(this.props.children, renderChild) }
                </ul>
            </div>
        );
    }
}

LandingPage.propTypes = { children: React.PropTypes.element.isRequired };

export default LandingPage;

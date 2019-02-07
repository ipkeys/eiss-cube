import React, { Component, Fragment } from 'react';
import compose from 'recompose/compose';
import { connect } from 'react-redux';
import { reset, submit, isPristine, isSubmitting, formValueSelector } from 'redux-form';
import { withStyles } from '@material-ui/core/styles';
import PropTypes from 'prop-types';
import {
    SimpleForm,
    BooleanInput,
    SelectInput,
    NumberInput,
    TextInput
} from 'react-admin';

import { SetupFormButton } from './SetupCube';

const styles = theme => ({
});

class RelayForm extends Component {

    constructor(props) {
        super(props);
        this.state = {
            data: props.data
        };

        this.handleSave = this.handleSave.bind(this);
    }
    
    componentDidUpdate(prevProps) {
		if (this.props.data !== prevProps.data) {
			let data = this.props.data; 

			this.setState({
				data
			});	
		}
	}

    handleSave = () => {
        const { submit } = this.props;
        submit('SetupCubeWizard');
    };

    render() {
        const { onSubmit, isPristine, isSubmitting, step, back, next, isRelayConnected } = this.props
        const { data } = this.state;

        return (
            <SimpleForm
                form='SetupCubeWizard'
                defaultValue={data}
                onSubmit={onSubmit}
                toolbar={null}
            >
                <BooleanInput label='Connected' source='relay.connected' margin='dense'/>
                {isRelayConnected &&
                    <Fragment>
                        <SelectInput label='To contacts' source="relay.contacts" choices={[
                            { id: 'NO', name: 'Normal Open' },
                            { id: 'NC', name: 'Normal Close' }
                        ]} margin='dense' fullWidth />
                        <NumberInput label='Load value (W)' source='relay.load' margin='dense' fullWidth />
                        <TextInput label='Label' source='relay.label' margin='dense' fullWidth />
                        <TextInput label='Description' source='relay.description' margin='dense' fullWidth />
                    </Fragment>
                }
                <SetupFormButton step={step} onSave={this.handleSave} onNext={next} onBack={back} pristine={isPristine} submitting={isSubmitting}/>
            </SimpleForm>
        );
    }

}

RelayForm.propTypes = {
    data: PropTypes.object
};

const selector = formValueSelector('SetupCubeWizard');

const mapStateToProps = state => ({
	isRelayConnected: selector(state, 'relay.connected'),
    isSubmitting: isSubmitting('SetupCubeWizard')(state),
    isPristine: isPristine('SetupCubeWizard')(state)
});

const mapDispatchToProps = {
	reset,
    submit
};

const enhance = compose(
    withStyles(styles)
);

export default enhance(
	connect(mapStateToProps, mapDispatchToProps)(
    	RelayForm
	)
);

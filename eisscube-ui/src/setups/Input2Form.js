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
    inline: { 
        display: 'inline-block',
        marginRight: theme.spacing.unit * 2, 
        minWidth: theme.spacing.unit * 32   
    }
});

class Input2Form extends Component {

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
        const { classes, onSubmit, isPristine, isSubmitting, step, onBack, onNext, isInput2Connected, input2SignalType } = this.props
        const { data } = this.state;

        return (
            <SimpleForm
                form='SetupCubeWizard'
                defaultValue={data}
                onSubmit={onSubmit}
                toolbar={null}
            >
                <BooleanInput label='Connected' source='input2.connected' className={classes.inline} margin='dense'/>
                {isInput2Connected &&
                    <Fragment>
                        <SelectInput label='Signal type' source="input2.signal" choices={[
                            { id: 'P', name: 'Pulses' },
                            { id: 'C', name: 'Cycles' }
                        ]} className={classes.inline} margin='dense' />
                        {input2SignalType && input2SignalType === "P" &&
                            <NumberInput label='Pulse factor (pulses per kWh)' source='input2.factor' className={classes.inline} margin='dense'/>
                        }
                        {input2SignalType && input2SignalType === "C" &&
                            <SelectInput label='Watch' source="input2.watch" choices={[
                                { id: 1, name: 'Relay 1' },
                                { id: 2, name: 'Relay 2' }
                            ]} className={classes.inline} margin='dense' />
                        }
                        <TextInput label='Label' source='input2.label' fullWidth margin='dense'/>
                        <TextInput label='Description' source='input2.description' fullWidth margin='dense'/>
                    </Fragment>
                }
                <SetupFormButton step={step} onSave={this.handleSave} onNext={onNext} onBack={onBack} pristine={isPristine} submitting={isSubmitting}/>
            </SimpleForm>
        );
    }

}

Input2Form.propTypes = {
    data: PropTypes.object
};

const selector = formValueSelector('SetupCubeWizard');

const mapStateToProps = state => ({
	isInput2Connected: selector(state, 'input2.connected'),
	input2SignalType: selector(state, 'input2.signal'),
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
    	Input2Form
	)
);

import { makeStyles, Theme } from '@material-ui/core/styles';
import ChevronLeft from '@material-ui/icons/ChevronLeft';
import { Field } from 'react-final-form';
import {
    Create,
    SimpleForm,
    NumberInput,
    SelectInput,
    ReferenceInput,
    AutocompleteInput,
    FormDataConsumer,
    TopToolbar,
    ListButton,
    required,
} from 'react-admin';
import { DateTimeMomentFormat } from '../../App';
import moment from 'moment';
import { NavCommandTitle } from '../common';
import CycleAndDutyCycleInput from './fields/CycleAndDutyCycleInput';
import { DateTimeFormInput } from './fields/DateTimePickerInput';
import { cmds, edges, checkCommandForInputCycle, checkCommandForRelayCycle, checkCommandForInputCount } from '.';

const useStyles = makeStyles((theme: Theme) => ({ 
    inline: { 
        display: 'inline-block',
        marginRight: theme.spacing(2), 
        minWidth: theme.spacing(32)   
    }
}));

const CommandCreateActions = ({data, basePath}: any) => {
    return (
        <TopToolbar>
            <ListButton basePath={basePath} label="Back" icon={<ChevronLeft />} />
        </TopToolbar>
    );
};

const validateCommandCreation = (values: any) => {
    const errors = {} as any;

    if (values.cycleAndDutyCycle) {
        const parts = values.cycleAndDutyCycle.split('/');
        if (parts[0] === '' || parseInt(parts[0], 10) < 1) {
            errors.cycleAndDutyCycle = ['Must be positive and more then 1 seconds'];
        }
    }

    if (values.completeCycle < 1) {
        errors.completeCycle = ['Must be positive more then 1 seconds'];
    }

    let s = (values.startTime) ? values.startTime : null;
    let e = (values.endTime) ? values.endTime : null;

    if (s !== null) {
        let sDate = moment(s);
        if (moment(sDate).isBefore()) {
            errors.startTime = ['Must be after of the current moment']
        }
    }
    if (e !== null) {
        let eDate = moment(e);
        if (moment(eDate).isBefore()) {
            errors.endTime = ['Must be after of the current moment']
        }
    }
    if (s !== null && e !== null) {
        let sDate = moment(s);
        let eDate = moment(e);
        if (moment(eDate).isSameOrBefore(sDate)) {
            errors.endTime = ['Must be after Start Date, Time'];
        }
    }

    return errors;
};
   
export const CommandCreate = (props: any) => {
    const classes = useStyles();

    return (
        <Create 
            title={<NavCommandTitle title='Create a new Command' />} 
            actions={<CommandCreateActions {...props} />}
            {...props}
        >
            <SimpleForm validate={ validateCommandCreation } redirect='list'>

                <ReferenceInput label='Device name' source='cubeID' reference='cubes' validate={required()} >
                    <AutocompleteInput optionText='name'/>
                </ReferenceInput>

                <SelectInput label='Command' source='command' choices={cmds.filter(value => value.id !== 'reboot')} validate={required()} />

                <FormDataConsumer>
                {({ formData, ...rest }) => checkCommandForRelayCycle(formData.command) &&
                    <Field name='cycleAndDutyCycle' component={CycleAndDutyCycleInput}
                        options={{ 
                            margin: 'dense',
                            variant: 'filled'
                        }}
                        {...rest} 
                    />
                 }
                </FormDataConsumer>

                <FormDataConsumer>
                {({ formData, ...rest }) => checkCommandForInputCount(formData.command) &&
                    <>
                        <SelectInput style={{ marginRight: 16 }} formClassName={classes.inline} label='Transition' source='transition' choices={edges} validate={required()} {...rest} />
                        <NumberInput formClassName={classes.inline} label='Cycle (sec)' source='completeCycle' step={'1'} {...rest} />
                    </>
                }
                </FormDataConsumer>

                <FormDataConsumer>
                {({ formData, ...rest }) => checkCommandForInputCycle(formData.command) &&
                    <SelectInput label='Transition' source='transition' choices={edges} validate={required()} {...rest} />
                }
                </FormDataConsumer>

                <DateTimeFormInput formClassName={classes.inline}
                    label='Start Date, Time' 
                    source='startTime' 
                    options={{ 
                        format: DateTimeMomentFormat, 
                        ampm: false,
                        margin: 'dense',
                        inputVariant: 'filled', 
                        clearable: true,
                        disablePast: true
                    }}
                />

                <DateTimeFormInput formClassName={classes.inline}
                    label='End Date, Time' 
                    source='endTime' 
                    options={{ 
                        format: DateTimeMomentFormat, 
                        ampm: false,
                        margin: 'dense',
                        inputVariant: 'filled', 
                        clearable: true,
                        disablePast: true
                    }} 
                />

            </SimpleForm>
        </Create>
    );
};

export default CommandCreate;

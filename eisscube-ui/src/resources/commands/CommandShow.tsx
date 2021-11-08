import { makeStyles, Theme } from '@material-ui/core/styles';
import ChevronLeft from '@material-ui/icons/ChevronLeft';
import {
    TopToolbar,
    SimpleShowLayout,
    DateField,
    TextField,
    SelectField,
    ReferenceField,
    ShowController,
    ShowView,
    Labeled,
    ListButton,
    usePermissions
} from 'react-admin';
import { AppDateTimeFormat } from '../../App';
import { isSuper } from '../common/Roles';
import { NavCommandTitle, DividerField } from '../common';
import CommandStatusField from './fields/CommandStatusField';
import CycleField from './fields/CycleField';
import DutyCycleField from './fields/DutyCycleField';
import { cmds, edges, checkCommandForInputCycle, checkCommandForRelayCycle, checkCommandForInputCount } from '.';

const useStyles = makeStyles((theme: Theme) => ({ 
    inlineField: { 
        display: 'inline-block',
        minWidth: theme.spacing(24)   
    },
    normalText: {
        minWidth: theme.spacing(32)   
    },
    longText: {
        minWidth: theme.spacing(64)   
    },
    break: {
        height: 0
    }
}));

const CommandShowActions = ({data, basePath}: any) => {
    return (
        <TopToolbar>
            <ListButton basePath={basePath} label="Back" icon={<ChevronLeft />} />
        </TopToolbar>
    );
};

export const CommandShow = (props: any) => {
    const classes = useStyles();
    const { permissions } = usePermissions();    

    return (
        <ShowController
            {...props}
        >
            {controllerProps => 
                <ShowView 
                    title={<NavCommandTitle title='Command' />}
                    actions={<CommandShowActions {...props} />}
                    {...props} 
                    {...controllerProps}
                >
                    <SimpleShowLayout>
                        <SelectField className={classes.inlineField} label='Command' source='command' choices={cmds} />

                        <ReferenceField className={classes.inlineField} label='for EISS™Cube' source='cubeID' reference='cubes' link='show'>
                            <TextField source='name' />
                        </ReferenceField>

                        {isSuper(permissions) &&
                            <ReferenceField className={classes.inlineField} source='group_id' label='Group' reference='grps' link={false} >
                                <TextField source='displayName' />
                            </ReferenceField>
                        }

                        <br className={classes.break} />

                        {controllerProps.record && controllerProps.record.startTime && 
                            <DateField className={classes.inlineField} label='Start date, time' source='startTime' showTime options={AppDateTimeFormat} />
                        }

                        {controllerProps.record && controllerProps.record.endTime && 
                            <DateField className={classes.inlineField} label='End date, time' source='endTime' showTime options={AppDateTimeFormat} />
                        }

                        {controllerProps.record && checkCommandForRelayCycle(controllerProps.record.command) && 
                            <>
                            <CycleField className={classes.inlineField} label='Cycle' source='completeCycle' suffix='sec' {...controllerProps}/>
                            <DutyCycleField className={classes.inlineField} label='Duty Cycle' source='dutyCycle' {...controllerProps}/>
                            </>
                        }

                        {controllerProps.record && checkCommandForInputCount(controllerProps.record.command) && 
                            <>
                            <Labeled label='Transition'>
                                <SelectField className={classes.inlineField} source='transition' choices={edges} translateChoice={false} {...controllerProps} {...props} />
                            </Labeled>
                            <CycleField className={classes.inlineField} label='Cycle' source='completeCycle' suffix='sec' {...controllerProps}/>
                            </>
                        }

                        {controllerProps.record && checkCommandForInputCycle(controllerProps.record.command) && 
                            <>
                            <Labeled label='Transition'>
                                <SelectField className={classes.inlineField} source='transition' choices={edges} translateChoice={false} {...controllerProps} {...props} />
                            </Labeled>
                            </>
                        }
                        
                        <DividerField />
                        
                        <CommandStatusField className={classes.inlineField} source='status' />
                        <DateField className={classes.inlineField} label='Created' source='created' showTime options={AppDateTimeFormat} />
                        <DateField className={classes.inlineField} label='Sent' source='sent' showTime options={AppDateTimeFormat} />
                        <DateField className={classes.inlineField} label='Received' source='received' showTime options={AppDateTimeFormat} />

                    </SimpleShowLayout>
                </ShowView>
            }
        </ShowController>
    );
};

export default CommandShow;

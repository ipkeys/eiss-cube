import ChevronLeft from '@mui/icons-material/ChevronLeft';
import { Box, Divider } from '@mui/material';
import {
	TopToolbar,
	SimpleShowLayout,
	DateField,
	TextField,
	SelectField,
	ReferenceField,
	Show,
	Labeled,
	ListButton,
	usePermissions,
	useShowController,
	useRecordContext
} from 'react-admin';
import { AppDateTimeFormat } from '../../App';
import { isSuper } from '../common/Roles';
import CommandStatusField from './fields/CommandStatusField';
import CycleField from './fields/CycleField';
import DutyCycleField from './fields/DutyCycleField';
import { cmds, edges, checkCommandForParams, checkCommandForInputCycle, checkCommandForRelayCycle, checkCommandForInputCount } from '.';
import find from 'lodash/find';

const CommandShowTitle = () => {
	const record = useRecordContext();
	if (!record) return null;

	const cmd = record.command && find(cmds, { 'id': record.command });

	return <span>Command - {cmd.name}</span>;
};

const CommandShowActions = () => (
	<TopToolbar>
		<ListButton label="Back" icon={<ChevronLeft />} />
	</TopToolbar>
);

export const CommandShow = () => {
	const { permissions } = usePermissions();
	const controllerProps = useShowController();

	return (
		<Show
			title={<CommandShowTitle />}
			actions={<CommandShowActions />}
			{...controllerProps}
		>
			<SimpleShowLayout>
			<Box display={'inline-flex'}>
				<Box flex={1} minWidth={'14em'} >
					<Labeled label='Command'>
						<SelectField source='command' choices={cmds} fullWidth />
					</Labeled>
				</Box>
				<Box flex={1} minWidth={'14em'} >
					<Labeled label='for EISS™Cube'>
						<ReferenceField source='cubeID' reference='cubes' link='show'>
							<TextField source='name' fullWidth />
						</ReferenceField>
					</Labeled>
				</Box>
				{isSuper(permissions) &&
					<Box flex={1} minWidth={'14em'} >
						<Labeled label='Group'>
							<ReferenceField source='group_id' reference='grps' link={false} >
								<TextField source='displayName' fullWidth />
							</ReferenceField>
						</Labeled>
					</Box>
				}
			</Box>

			{checkCommandForParams(controllerProps.record) &&
			<Box display={'inline-flex'}>
				{checkCommandForRelayCycle(controllerProps.record.command) &&
				<Box flex={1} minWidth={'14em'} >
					<CycleField label='Cycle' source='completeCycle' suffix='sec' {...controllerProps}/>
				</Box>
				}
				{checkCommandForRelayCycle(controllerProps.record.command) &&
				<Box flex={1} minWidth={'14em'} >
					<DutyCycleField label='Duty Cycle' source='dutyCycle' {...controllerProps}/>
				</Box>
				}

				{checkCommandForInputCount(controllerProps.record.command) &&
				<Box flex={1} minWidth={'14em'} >
					<Labeled label='Transition'>
						<SelectField source='transition' choices={edges} translateChoice={false} {...controllerProps} />
					</Labeled>
				</Box>
				}
				{checkCommandForInputCount(controllerProps.record.command) &&
				<Box flex={1} minWidth={'14em'} >
					<CycleField label='Cycle' source='completeCycle' suffix='sec' {...controllerProps}/>
				</Box>
				}

				{checkCommandForInputCycle(controllerProps.record.command) &&
				<Box flex={1} minWidth={'14em'} >
					<Labeled label='Transition'>
						<SelectField source='transition' choices={edges} translateChoice={false} {...controllerProps} />
					</Labeled>
				</Box>
				}

				{controllerProps.record.startTime &&
				<Box flex={1} minWidth={'14em'} >
					<Labeled label='Start date, time'>
						<DateField source='startTime' showTime options={AppDateTimeFormat} />
					</Labeled>
				</Box>
				}

				{controllerProps.record.endTime &&
				<Box flex={1} minWidth={'14em'} >
					<Labeled label='End date, time'>
						<DateField source='endTime' showTime options={AppDateTimeFormat} />
					</Labeled>
				</Box>
				}
			</Box>
			}

			<Divider />

			<Box display={'inline-flex'}>
				<Box flex={1} minWidth={'14em'} >
					<Labeled label='Status'>
						<CommandStatusField />
					</Labeled>
				</Box>
				<Box flex={1} minWidth={'14em'} >
					<Labeled label='Created'>
						<DateField source='created' showTime options={AppDateTimeFormat} />
					</Labeled>
				</Box>
			</Box>

			<Box display={'inline-flex'}>
				<Box flex={1} minWidth={'14em'} >
					<Labeled label='Sent'>
						<DateField source='sent' showTime options={AppDateTimeFormat} emptyText="waiting..." />
					</Labeled>
				</Box>
				<Box flex={1} minWidth={'14em'} >
					<Labeled label='Received'>
						<DateField source='received' showTime options={AppDateTimeFormat} emptyText="waiting..." />
					</Labeled>
				</Box>
			</Box>

			</SimpleShowLayout>
		</Show>
	);
};

export default CommandShow;

export type MessageType = "TEXT" | "IMAGE" | "CAROUSEL";
export type AudienceType = "FOLLOWER" | "NON_FOLLOWER";
export type SendResult = "SUCCESS" | "FAILED";

export interface CarouselItemInput {
	imageUrl: string;
	title?: string;
	subtitle?: string;
	buttonText?: string;
	buttonUrl?: string;
}

export interface MessageInput {
	messageType: MessageType;
	textContent?: string;
	imageUrl?: string;
	carouselItems?: CarouselItemInput[];
}

export interface CreateTemplateRequest {
	name: string;
	postId: number;
	dispatchTime?: string | null;
	keywords?: string[];
	dmKeyword?: string;
	commentReplyText?: string;
	followerMessages?: MessageInput[];
	nonFollowerMessages?: MessageInput[];
}

export interface TemplateResponse {
	id: number;
	name: string;
	postId: number;
	dispatchTime: string | null;
	keywords: string[];
	dmKeyword: string | null;
	commentReplyText: string | null;
	createdAt: string;
}

export interface SendLogResponse {
	id: number;
	templateId: number;
	templateName: string;
	audienceType: AudienceType | null;
	recipientPlatformUserId: string;
	result: SendResult;
	failureReason: string | null;
	createdAt: string;
}
